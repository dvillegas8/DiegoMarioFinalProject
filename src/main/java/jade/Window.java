package jade;

import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window {
    private int width;
    private int height;
    private String title;
    private long glfwWindow;
    // Only have one instance of a window
    private static Window window = null;
    public float r, g, b, a;
    private boolean fadeToBlack;
    private static Scene currentScene;
    // Constructor
    private Window(){
        this.width = 1920;
        this.height = 1080;
        this.title = "Mario";
        r = 1;
        g = 1;
        b = 1;
        a = 1;
    }
    public static void changeScene(int newScene){
        switch(newScene){
            case 0:
                currentScene = new LevelEditorScene();
                currentScene.init();
                currentScene.start();
                break;
            case 1:
                currentScene = new LevelScene();
                currentScene.init();
                currentScene.start();
                break;
            default:
                assert false : "Unknown scene" + newScene + "'";
                break;
        }
    }
    public static Window get(){
        if(Window.window == null){
            Window.window = new Window();
        }
        return Window.window;
    }

    public static Scene getScene(){
        return get().currentScene;
    }
    public void run(){
        System.out.println("Hello LWJGL" + Version.getVersion() + "!");

        init();
        loop();

        // Free the memory
        glfwFreeCallbacks(glfwWindow);
        glfwDestroyWindow(glfwWindow);

        // Terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();

    }
    public void init(){
        // Set up and error callback, where the GLFW will print to if there is an error
        GLFWErrorCallback.createPrint(System.err).set();
        // Initialize GLFW
        if(!glfwInit()){
            // Throw error if it does not succeed
            throw new IllegalStateException("Unable to initialize GLFW.");
        }
        // Configure GLFW, WindowHints are basically if you want your window to be resizeable
        // If you want a default close operation, do you want it to be visible
        glfwDefaultWindowHints();
        // Not Visible until we are done creating it
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        // When the Window Starts, it is in the maximized position
        glfwWindowHint(GLFW_MAXIMIZED, GLFW_TRUE);
        // 2. Set OpenGL version & profile (your code goes here)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE); // For macOS compatibility
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        // Create the window, GLFW will use the hints to create the window
        // This long is a memory address where the window is in the memory space
        glfwWindow = glfwCreateWindow(this.width, this.height, this.title, NULL, NULL);
        if(glfwWindow == NULL){
            throw new IllegalStateException("Failed to create the GLFW window.");
        }
        // :: shorthand for lambda functions, basically forward your position call back to the mousePosCallBack function
        glfwSetCursorPosCallback(glfwWindow, MouseListener::mousePosCallback);
        glfwSetMouseButtonCallback(glfwWindow, MouseListener::mouseButtonCallback);
        glfwSetScrollCallback(glfwWindow, MouseListener::mouseScrollCallback);
        glfwSetKeyCallback(glfwWindow, KeyListener::keyCallback);
        // Make the OpenGL context current
        glfwMakeContextCurrent(glfwWindow);
        // Enable v-sync, buffer swapping, no wait time between the frames, swap every frame
        glfwSwapInterval(1);
        // Make the window visible
        glfwShowWindow(glfwWindow);

        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities();

        // Enable blending in Open GL
        glEnable(GL_BLEND);
        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

        Window.changeScene(0);
    }
    public void loop(){
        // The time the frame began
        float beginTime = (float)glfwGetTime();
        // The time the frame ended
        float endTime;
        float dt = -1.0f;
        while(!glfwWindowShouldClose(glfwWindow)){
            // Poll Events
            glfwPollEvents();
            glClearColor(r, g, b, a);
            // Tells Open GL how to clear the buffer
            glClear(GL_COLOR_BUFFER_BIT);

            if(dt >= 0){
                currentScene.update(dt);
            }

            glfwSwapBuffers(glfwWindow);
            endTime = (float)glfwGetTime();
            // Time between frames
            dt = endTime - beginTime;
            beginTime = endTime;
        }
    }
}
