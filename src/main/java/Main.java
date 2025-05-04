import jade.Window;

public class Main {
    public static void main(String[] args){
        // Only have a single window object, the game engine only has one window
        Window window = Window.get();
        window.run();
    }

}
