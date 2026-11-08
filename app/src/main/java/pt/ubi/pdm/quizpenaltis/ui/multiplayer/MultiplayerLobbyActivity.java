package pt.ubi.pdm.quizpenaltis.ui.multiplayer;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import pt.ubi.pdm.quizpenaltis.R;
import pt.ubi.pdm.quizpenaltis.model.Player;
import pt.ubi.pdm.quizpenaltis.model.PlayerCatalog;
import pt.ubi.pdm.quizpenaltis.multiplayer.AuthManager;
import pt.ubi.pdm.quizpenaltis.multiplayer.FirebaseRoomRepository;
import pt.ubi.pdm.quizpenaltis.multiplayer.LobbyFlow;
import pt.ubi.pdm.quizpenaltis.multiplayer.PlayerInfo;
import pt.ubi.pdm.quizpenaltis.multiplayer.PlayerName;
import pt.ubi.pdm.quizpenaltis.multiplayer.Role;
import pt.ubi.pdm.quizpenaltis.multiplayer.RoomCode;
import pt.ubi.pdm.quizpenaltis.multiplayer.RoomRepository;
import pt.ubi.pdm.quizpenaltis.multiplayer.RoomSnapshot;
import pt.ubi.pdm.quizpenaltis.steps.StepCounter;
import pt.ubi.pdm.quizpenaltis.ui.game.GameActivity;
import pt.ubi.pdm.quizpenaltis.ui.player.PlayerSelectionActivity;
import pt.ubi.pdm.quizpenaltis.util.Constants;
import pt.ubi.pdm.quizpenaltis.util.SystemBars;

/**
 * Sala de espera de "Jogar com um amigo": escolher jogador (sem cobrança), criar sala ou entrar com código,
 * esperar pelo amigo e abrir o jogo quando a partida começa.
 *
 * <p>Pontos: NADA é cobrado aqui antes de {@code status == "playing"}. Nesse momento cada telemóvel cobra
 * o seu jogador com {@code StepCounter.spendPoints} (uma só vez, ver {@link LobbyFlow}) e abre o
 * {@link GameActivity}, que cria o seu próprio repositório e volta a escutar a sala. Aqui para-se o sinal
 * de vida e a escuta antes de abrir o jogo, para não haver dois sinais de vida.
 *
 * <p>Limites conhecidos:
 * <ul>
 *   <li>Um convidado que sai NÃO consegue libertar o lugar (as regras proíbem apagar {@code meta.guest}):
 *       simplesmente para o sinal de vida e sai. É o anfitrião que, ao ver o convidado sem sinal, apaga a
 *       sala ({@code deleteRoom}) e o utilizador cria uma nova.</li>
 *   <li>Se a cobrança falhar ao ver {@code playing} (corrida rara à meia-noite) não dá para escrever o
 *       adversário como vencedor (as regras só deixam escrever {@code winner} com o próprio papel): para-se
 *       o sinal de vida, sai-se e mostra-se a mensagem; a deteção de desistência do adversário (Tarefa 8)
 *       dá-lhe a vitória.</li>
 * </ul>
 */
public class MultiplayerLobbyActivity extends AppCompatActivity {

    private enum Pending { NONE, CREATE, JOIN }

    private View setupPanel, roomPanel, rowFriend, progressRoom;
    private TextView tvMessage, tvChosenPlayer, tvRoomTitle, tvCodeCaption, tvRoomCode, tvYou, tvFriend;
    private EditText etName, etCode;
    private Button btnPickPlayer, btnCreate, btnJoin, btnLeave;
    private ImageView imgYou, imgFriend;

    private String chosenPlayer;
    private int chosenCost;
    private Pending pending = Pending.NONE;
    private boolean busy;
    private boolean destroyed;

    // Sala em que estamos (null enquanto no ecrã de criar/entrar)
    private RoomRepository repo;
    private String roomCode;
    private Role role;
    private LobbyFlow flow;
    /** O jogo já foi aberto: o repositório do jogo toma conta da sala, não se sai dela. */
    private boolean handedOver;

    private final ActivityResultLauncher<Intent> pickPlayer =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                Pending action = pending;
                pending = Pending.NONE;
                Intent data = result.getData();
                if (result.getResultCode() != RESULT_OK || data == null) return;
                String name = data.getStringExtra(Constants.EXTRA_SELECTED_PLAYER);
                if (name == null) return;
                chosenPlayer = name;
                chosenCost = data.getIntExtra(Constants.EXTRA_PLAYER_COST, 0);
                renderChosenPlayer();
                // O saldo é relido na hora (nunca copiado do ecrã de escolha)
                if (action == Pending.CREATE) startCreate();
                else if (action == Pending.JOIN) startJoin();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_multiplayer_lobby);
        SystemBars.applyPadding(findViewById(R.id.lobbyScroll));

        setupPanel = findViewById(R.id.setupPanel);
        roomPanel = findViewById(R.id.roomPanel);
        rowFriend = findViewById(R.id.rowFriend);
        progressRoom = findViewById(R.id.progressRoom);
        tvMessage = findViewById(R.id.tvMessage);
        tvChosenPlayer = findViewById(R.id.tvChosenPlayer);
        tvRoomTitle = findViewById(R.id.tvRoomTitle);
        tvCodeCaption = findViewById(R.id.tvCodeCaption);
        tvRoomCode = findViewById(R.id.tvRoomCode);
        tvYou = findViewById(R.id.tvYou);
        tvFriend = findViewById(R.id.tvFriend);
        etName = findViewById(R.id.etName);
        etCode = findViewById(R.id.etCode);
        btnPickPlayer = findViewById(R.id.btnPickPlayer);
        btnCreate = findViewById(R.id.btnCreate);
        btnJoin = findViewById(R.id.btnJoin);
        btnLeave = findViewById(R.id.btnLeave);
        imgYou = findViewById(R.id.imgYou);
        imgFriend = findViewById(R.id.imgFriend);

        btnPickPlayer.setOnClickListener(v -> openSelection(Pending.NONE));
        btnCreate.setOnClickListener(v -> onCreateClicked());
        btnJoin.setOnClickListener(v -> onJoinClicked());
        btnLeave.setOnClickListener(v -> leaveRoomAndReset());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (roomCode != null) {
                    leaveRoomAndReset();
                } else {
                    finish();
                }
            }
        });
    }

    // ---------------------------------------------------------------- ecrã de criar / entrar

    private void onCreateClicked() {
        if (busy) return;
        clearMessage();
        if (!hasEnoughPoints()) return;
        if (chosenPlayer == null) {
            openSelection(Pending.CREATE);
        } else {
            startCreate();
        }
    }

    private void onJoinClicked() {
        if (busy) return;
        clearMessage();
        // Validação local do código ANTES de tocar na rede
        String code = RoomCode.normalize(etCode.getText().toString());
        if (!RoomCode.isValid(code)) {
            showMessage(R.string.lobby_err_invalid_code);
            return;
        }
        etCode.setText(code);
        if (!hasEnoughPoints()) return;
        if (chosenPlayer == null) {
            openSelection(Pending.JOIN);
        } else {
            startJoin();
        }
    }

    private boolean hasEnoughPoints() {
        int minimum = PlayerCatalog.cheapestCost();
        if (StepCounter.getTodayPoints(this) < minimum) {
            showMessage(getString(R.string.lobby_err_need_points, minimum));
            return false;
        }
        return true;
    }

    private void openSelection(Pending next) {
        pending = next;
        Intent intent = new Intent(this, PlayerSelectionActivity.class);
        intent.putExtra(Constants.EXTRA_MODE, Constants.MODE_MULTI);
        pickPlayer.launch(intent);
    }

    private void renderChosenPlayer() {
        if (chosenPlayer == null) {
            tvChosenPlayer.setText(R.string.lobby_player_none);
            btnPickPlayer.setText(R.string.lobby_btn_pick);
        } else {
            tvChosenPlayer.setText(getString(R.string.lobby_player_chosen, chosenPlayer, chosenCost));
            btnPickPlayer.setText(R.string.lobby_btn_change);
        }
    }

    private PlayerInfo buildMyInfo(String uid) {
        String name = PlayerName.sanitize(etName.getText().toString());
        etName.setText(name);
        return new PlayerInfo(uid, name, chosenPlayer, chosenCost, false, 0L);
    }

    private void setBusy(boolean value) {
        busy = value;
        btnCreate.setEnabled(!value);
        btnJoin.setEnabled(!value);
        btnPickPlayer.setEnabled(!value);
    }

    private void startCreate() {
        setBusy(true);
        showStatus(R.string.lobby_connecting);
        AuthManager.signIn(new AuthManager.Callback() {
            @Override public void onSuccess(String uid) {
                if (destroyed) return;
                RoomRepository r = new FirebaseRoomRepository();
                r.createRoom(buildMyInfo(uid), code -> {
                    if (destroyed) return;
                    if (code == null) {
                        setBusy(false);
                        showMessage(R.string.lobby_err_create);
                        return;
                    }
                    enterRoom(r, code, Role.HOST);
                });
            }

            @Override public void onError(Exception e) {
                if (destroyed) return;
                setBusy(false);
                showMessage(R.string.lobby_err_sign_in);
            }
        });
    }

    private void startJoin() {
        final String code = RoomCode.normalize(etCode.getText().toString());
        if (!RoomCode.isValid(code)) {
            showMessage(R.string.lobby_err_invalid_code);
            return;
        }
        setBusy(true);
        showStatus(R.string.lobby_connecting);
        AuthManager.signIn(new AuthManager.Callback() {
            @Override public void onSuccess(String uid) {
                if (destroyed) return;
                RoomRepository r = new FirebaseRoomRepository();
                r.joinRoom(code, buildMyInfo(uid), result -> {
                    if (destroyed) return;
                    if (result == RoomRepository.JoinResult.OK) {
                        enterRoom(r, code, Role.GUEST);
                        return;
                    }
                    setBusy(false);
                    showMessage(joinErrorMessage(result));
                });
            }

            @Override public void onError(Exception e) {
                if (destroyed) return;
                setBusy(false);
                showMessage(R.string.lobby_err_sign_in);
            }
        });
    }

    private static int joinErrorMessage(RoomRepository.JoinResult result) {
        switch (result) {
            case NOT_FOUND: return R.string.lobby_err_not_found;
            case FULL: return R.string.lobby_err_full;
            case WRONG_VERSION: return R.string.lobby_err_version;
            default: return R.string.lobby_err_no_connection;
        }
    }

    // ---------------------------------------------------------------- dentro da sala

    /** O registo do jogador já existe na sala: sinal de vida e escuta (nesta ordem, só depois de criar/entrar). */
    private void enterRoom(RoomRepository repository, String code, Role myRole) {
        repo = repository;
        roomCode = code;
        role = myRole;
        flow = new LobbyFlow(myRole, chosenCost);
        clearMessage();

        setupPanel.setVisibility(View.GONE);
        roomPanel.setVisibility(View.VISIBLE);
        tvRoomCode.setText(code);
        showStage(LobbyFlow.Stage.WAITING_FRIEND, null);

        repo.startHeartbeat(code, myRole);
        repo.listen(code, myRole, new RoomRepository.Listener() {
            @Override public void onRoom(RoomSnapshot s) {
                if (!destroyed && repo != null) onRoomChanged(s);
            }

            @Override public void onError(String message) {
                if (!destroyed && repo != null) showMessage(R.string.lobby_err_sync);
            }
        });
    }

    private void onRoomChanged(RoomSnapshot s) {
        LobbyFlow.Stage stage = LobbyFlow.stageOf(s);
        if (stage != LobbyFlow.Stage.CLOSED) showStage(stage, s);

        // O saldo é relido do StepCounter de cada vez que é preciso (nunca guardado)
        LobbyFlow.Action action = flow.decide(s, StepCounter.getTodayPoints(this), repo.serverNow());
        switch (action) {
            case SET_READY:
                repo.setReady(roomCode, role);
                break;
            case START_MATCH:
                final LobbyFlow current = flow;
                repo.startMatch(roomCode, started -> {
                    // Recusado (p.ex. sinal de vida antigo): volta a tentar no próximo estado da sala
                    if (!destroyed && Boolean.FALSE.equals(started)) current.startRefused();
                });
                break;
            case CHARGE_AND_LAUNCH:
                chargeAndLaunch();
                break;
            case CANNOT_PAY:
                // Não se marca ready. O amigo fica sem sinal deste telemóvel e a sala fecha-se do lado dele.
                leaveRoomAndReset();
                showMessage(R.string.lobby_err_cannot_pay);
                break;
            case OPPONENT_GONE:
                // Anfitrião: apaga a sala (o convidado não consegue libertar o lugar). Convidado: só sai.
                if (role == Role.HOST) repo.deleteRoom(roomCode);
                leaveRoomAndReset();
                showMessage(R.string.lobby_err_friend_left);
                break;
            case ROOM_CLOSED:
                leaveRoomAndReset();
                showMessage(R.string.lobby_err_room_closed);
                break;
            default:
                break;
        }
    }

    /** Chamado uma só vez (guarda em {@link LobbyFlow}) quando a sala passa a "playing". */
    private void chargeAndLaunch() {
        // Entrega a sala ao jogo: sem sinal de vida nem escuta duplicados
        repo.stopHeartbeat();
        repo.stopListening();

        if (!StepCounter.spendPoints(this, chosenCost)) {
            // Corrida rara (meia-noite entre ready e playing). Não dá para declarar o adversário vencedor
            // (as regras só deixam escrever o próprio papel): sai-se e a desistência é detetada do outro lado.
            repo.leave(roomCode, role);
            resetToSetup();
            showMessage(R.string.lobby_err_charge_failed);
            return;
        }

        handedOver = true;
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra(Constants.EXTRA_MODE, Constants.MODE_MULTI);
        intent.putExtra(Constants.EXTRA_ROOM_CODE, roomCode);
        intent.putExtra(Constants.EXTRA_ROLE, role.key());
        intent.putExtra(Constants.EXTRA_SELECTED_PLAYER, chosenPlayer);
        intent.putExtra(Constants.EXTRA_PLAYER_COST, chosenCost);
        startActivity(intent);
        finish();
    }

    private void showStage(LobbyFlow.Stage stage, RoomSnapshot s) {
        boolean host = role == Role.HOST;
        int title;
        switch (stage) {
            case FRIEND_JOINED:
                title = R.string.lobby_joined_title;
                break;
            case STARTING:
                title = R.string.lobby_starting_title;
                break;
            default:
                title = R.string.lobby_waiting_title;
                break;
        }
        tvRoomTitle.setText(title);
        // O código grande serve ao anfitrião para o dizer ao amigo; depois de o amigo entrar já não é preciso
        boolean showCode = host && stage == LobbyFlow.Stage.WAITING_FRIEND;
        tvCodeCaption.setVisibility(showCode ? View.VISIBLE : View.GONE);
        progressRoom.setVisibility(stage == LobbyFlow.Stage.STARTING ? View.GONE : View.VISIBLE);

        PlayerInfo mine = s != null ? s.player(role) : null;
        PlayerInfo friend = s != null ? s.player(role.other()) : null;
        renderPlayerRow(tvYou, imgYou, R.string.lobby_you,
                mine != null ? mine.name : PlayerName.sanitize(etName.getText().toString()),
                mine != null && mine.player != null ? mine.player : chosenPlayer);
        if (friend != null) {
            rowFriend.setVisibility(View.VISIBLE);
            renderPlayerRow(tvFriend, imgFriend, R.string.lobby_friend, friend.name, friend.player);
        } else {
            rowFriend.setVisibility(View.GONE);
        }
    }

    private void renderPlayerRow(TextView text, ImageView image, int label, String name, String playerName) {
        String detail = playerName != null ? name + " · " + playerName : name;
        text.setText(getString(R.string.lobby_friend_card, getString(label), detail));
        Player p = null;
        for (Player candidate : PlayerCatalog.ALL) {
            if (candidate.getName().equals(playerName)) p = candidate;
        }
        if (p != null) {
            image.setImageResource(p.getImageResource());
        } else {
            image.setImageDrawable(null);
        }
    }

    // ---------------------------------------------------------------- sair

    /** Sai da sala (sem custo) e volta ao ecrã de criar/entrar. */
    private void leaveRoomAndReset() {
        if (repo != null && roomCode != null && !handedOver) {
            // Anfitrião numa sala em espera: o repositório apaga-a. Convidado: só para (não pode libertar o lugar).
            repo.leave(roomCode, role);
        }
        resetToSetup();
    }

    private void resetToSetup() {
        repo = null;
        roomCode = null;
        role = null;
        flow = null;
        roomPanel.setVisibility(View.GONE);
        setupPanel.setVisibility(View.VISIBLE);
        setBusy(false);
        clearMessage();
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        // Fechar o ecrã a meio da espera = sair da sala, sem custo (nada foi cobrado)
        if (repo != null && roomCode != null && !handedOver) repo.leave(roomCode, role);
        repo = null;
        super.onDestroy();
    }

    // ---------------------------------------------------------------- mensagens

    private void showMessage(int resId) {
        showMessage(getString(resId));
    }

    private void showMessage(String text) {
        tvMessage.setBackgroundResource(R.drawable.ds_banner_error);
        tvMessage.setText(text);
        tvMessage.setVisibility(View.VISIBLE);
    }

    private void showStatus(int resId) {
        tvMessage.setBackgroundResource(R.drawable.ds_banner_info);
        tvMessage.setText(resId);
        tvMessage.setVisibility(View.VISIBLE);
    }

    private void clearMessage() {
        tvMessage.setVisibility(View.GONE);
    }
}
