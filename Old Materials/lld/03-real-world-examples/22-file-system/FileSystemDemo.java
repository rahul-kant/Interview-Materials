/**
 * File System Demo
 */
public class FileSystemDemo {
    
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║      FILE SYSTEM DEMO                    ║");
        System.out.println("╚══════════════════════════════════════════╝\n");
        
        FileSystem fs = new FileSystem();
        
        // Create directories and files
        System.out.println("=== Creating File Structure ===\n");
        fs.mkdir("docs");
        fs.mkdir("projects");
        fs.touch("readme.txt");
        
        fs.ls();
        
        // Navigate
        System.out.println("\n=== Navigating ===\n");
        fs.cd("docs");
        fs.touch("notes.txt");
        fs.write("notes.txt", "Hello World!");
        fs.read("notes.txt");
        
        System.out.println("\n💡 Key Concepts:");
        System.out.println("   - Composite pattern (tree structure)");
        System.out.println("   - File/Directory operations");
        System.out.println("   - Path navigation");
    }
}
