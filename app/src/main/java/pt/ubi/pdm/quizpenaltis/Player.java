package pt.ubi.pdm.quizpenaltis;

public class Player {
    private String name;
    private int cost;
    private int imageResource;
    private String difficulty;

    public Player(String name, int cost, int imageResource, String difficulty) {
        this.name = name != null ? name : "Jogador";
        this.cost = cost;
        this.imageResource = imageResource;
        this.difficulty = difficulty != null ? difficulty : "Normal";
    }

    public String getName() { return name; }
    public int getCost() { return cost; }
    public int getImageResource() { return imageResource; }
    public String getDifficulty() { return difficulty; }
}