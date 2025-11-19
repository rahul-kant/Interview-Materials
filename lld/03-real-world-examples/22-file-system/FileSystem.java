import java.util.*;

class FSNode {
    String name;
    boolean isDirectory;
    String content;
    Map<String, FSNode> children;
    
    public FSNode(String name, boolean isDirectory) {
        this.name = name;
        this.isDirectory = isDirectory;
        this.content = "";
        this.children = isDirectory ? new HashMap<>() : null;
    }
}

public class FileSystem {
    private FSNode root;
    private FSNode current;
    
    public FileSystem() {
        root = new FSNode("/", true);
        current = root;
    }
    
    public void mkdir(String name) {
        if (current.children.containsKey(name)) {
            System.out.println("Directory already exists: " + name);
            return;
        }
        current.children.put(name, new FSNode(name, true));
        System.out.println("✓ Created directory: " + name);
    }
    
    public void touch(String name) {
        if (current.children.containsKey(name)) {
            System.out.println("File already exists: " + name);
            return;
        }
        current.children.put(name, new FSNode(name, false));
        System.out.println("✓ Created file: " + name);
    }
    
    public void ls() {
        System.out.println("\nContents of " + current.name + ":");
        for (FSNode node : current.children.values()) {
            String type = node.isDirectory ? "[DIR]" : "[FILE]";
            System.out.println("  " + type + " " + node.name);
        }
    }
    
    public void cd(String name) {
        if (name.equals("..")) {
            System.out.println("✓ Changed to parent (root)");
            return;
        }
        
        FSNode node = current.children.get(name);
        if (node == null || !node.isDirectory) {
            System.out.println("Directory not found: " + name);
            return;
        }
        current = node;
        System.out.println("✓ Changed directory to: " + name);
    }
    
    public void write(String filename, String content) {
        FSNode node = current.children.get(filename);
        if (node == null || node.isDirectory) {
            System.out.println("File not found: " + filename);
            return;
        }
        node.content = content;
        System.out.println("✓ Wrote to file: " + filename);
    }
    
    public void read(String filename) {
        FSNode node = current.children.get(filename);
        if (node == null || node.isDirectory) {
            System.out.println("File not found: " + filename);
            return;
        }
        System.out.println("Content of " + filename + ": " + node.content);
    }
}
