/**
 * StudentDemo - Demonstrates how to create and use Student objects
 * 
 * This class shows:
 * - Creating objects using different constructors
 * - Calling methods on objects
 * - Working with multiple objects
 * - Object interaction
 */
public class StudentDemo {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("    STUDENT MANAGEMENT SYSTEM DEMO");
        System.out.println("========================================\n");
        
        // Example 1: Creating a student object using full constructor
        System.out.println("Example 1: Creating Student with Full Constructor");
        System.out.println("--------------------------------------------------");
        Student student1 = new Student("Alice Johnson", 101, 20, 85.5);
        student1.displayInfo();
        
        // Example 2: Creating a student with partial constructor
        System.out.println("\n\nExample 2: Creating Student with Partial Constructor");
        System.out.println("-----------------------------------------------------");
        Student student2 = new Student("Bob Smith", 102, 21);
        student2.displayInfo();
        
        // Example 3: Using setters to update student information
        System.out.println("\n\nExample 3: Updating Student Grade");
        System.out.println("----------------------------------");
        student2.updateGrade(72.0);
        student2.displayInfo();
        
        // Example 4: Creating student with default constructor
        System.out.println("\n\nExample 4: Creating Student with Default Constructor");
        System.out.println("-----------------------------------------------------");
        Student student3 = new Student();
        System.out.println("Before setting values:");
        student3.displayInfo();
        
        // Now set values using setters
        student3.setName("Charlie Brown");
        student3.setAge(19);
        student3.setGrade(91.0);
        System.out.println("\nAfter setting values:");
        student3.displayInfo();
        
        // Example 5: Using getter methods
        System.out.println("\n\nExample 5: Using Getter Methods");
        System.out.println("--------------------------------");
        System.out.println("Student 1 Name: " + student1.getName());
        System.out.println("Student 1 Roll Number: " + student1.getRollNumber());
        System.out.println("Student 1 Age: " + student1.getAge());
        System.out.println("Student 1 Grade: " + student1.getGrade() + "%");
        System.out.println("Student 1 Letter Grade: " + student1.getLetterGrade());
        
        // Example 6: Checking if student passed
        System.out.println("\n\nExample 6: Checking Pass/Fail Status");
        System.out.println("-------------------------------------");
        System.out.println(student1.getName() + " has " + 
                          (student1.hasPassed() ? "PASSED" : "FAILED"));
        
        // Create a failing student for demonstration
        Student student4 = new Student("David Lee", 104, 20, 35.0);
        System.out.println(student4.getName() + " has " + 
                          (student4.hasPassed() ? "PASSED" : "FAILED"));
        
        // Example 7: Using toString() method
        System.out.println("\n\nExample 7: Using toString() Method");
        System.out.println("-----------------------------------");
        System.out.println(student1.toString());
        System.out.println(student2.toString());
        System.out.println(student3.toString());
        
        // Example 8: Comparing students
        System.out.println("\n\nExample 8: Comparing Students");
        System.out.println("------------------------------");
        int comparison = student1.compareGrades(student2);
        if (comparison > 0) {
            System.out.println(student1.getName() + " has a higher grade than " + 
                              student2.getName());
        } else if (comparison < 0) {
            System.out.println(student1.getName() + " has a lower grade than " + 
                              student2.getName());
        } else {
            System.out.println(student1.getName() + " and " + student2.getName() + 
                              " have the same grade");
        }
        
        // Example 9: Validation in action
        System.out.println("\n\nExample 9: Input Validation");
        System.out.println("----------------------------");
        Student student5 = new Student("Eve Wilson", 105, 22, 88.0);
        
        System.out.println("\nTrying to set invalid grade (150):");
        student5.updateGrade(150.0);  // Should show error
        
        System.out.println("\nTrying to set invalid age (150):");
        student5.setAge(150);  // Should show error
        
        System.out.println("\nSetting valid values:");
        student5.updateGrade(92.0);  // Should succeed
        student5.setAge(23);  // Should succeed
        student5.displayInfo();
        
        // Example 10: Working with array of students
        System.out.println("\n\nExample 10: Array of Students");
        System.out.println("------------------------------");
        Student[] classroom = new Student[3];
        classroom[0] = new Student("Frank Miller", 201, 20, 78.5);
        classroom[1] = new Student("Grace Lee", 202, 21, 92.0);
        classroom[2] = new Student("Henry Davis", 203, 19, 65.5);
        
        System.out.println("Classroom Summary:");
        double totalGrade = 0;
        int passedCount = 0;
        
        for (int i = 0; i < classroom.length; i++) {
            Student s = classroom[i];
            System.out.println((i + 1) + ". " + s.getName() + 
                              " - Grade: " + s.getGrade() + "% (" + 
                              s.getLetterGrade() + ")");
            totalGrade += s.getGrade();
            if (s.hasPassed()) {
                passedCount++;
            }
        }
        
        double averageGrade = totalGrade / classroom.length;
        System.out.println("\nClass Statistics:");
        System.out.println("Average Grade: " + String.format("%.2f", averageGrade) + "%");
        System.out.println("Students Passed: " + passedCount + "/" + classroom.length);
        
        // Example 11: Finding top student
        System.out.println("\n\nExample 11: Finding Top Performer");
        System.out.println("----------------------------------");
        Student topStudent = classroom[0];
        for (Student s : classroom) {
            if (s.compareGrades(topStudent) > 0) {
                topStudent = s;
            }
        }
        System.out.println("Top Performer: " + topStudent.getName() + 
                          " with " + topStudent.getGrade() + "%");
        
        // Summary
        System.out.println("\n\n========================================");
        System.out.println("           KEY LEARNINGS");
        System.out.println("========================================");
        System.out.println("1. Objects are created using 'new' keyword");
        System.out.println("2. Constructors initialize object state");
        System.out.println("3. Methods define object behavior");
        System.out.println("4. Each object has its own copy of instance variables");
        System.out.println("5. Getters/Setters provide controlled access");
        System.out.println("6. Validation ensures data integrity");
        System.out.println("7. Multiple objects can interact with each other");
        System.out.println("========================================\n");
    }
}
