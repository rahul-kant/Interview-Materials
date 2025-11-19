/**
 * Student Class - Demonstrates basic OOP concepts
 * 
 * This class represents a student with basic attributes and behaviors.
 * It shows:
 * - Instance variables (attributes/properties)
 * - Constructor (for initializing objects)
 * - Methods (behavior)
 * - The 'this' keyword usage
 */
public class Student {
    // Instance Variables (Private for encapsulation)
    // Each Student object will have its own copy of these variables
    private String name;
    private int rollNumber;
    private int age;
    private double grade;
    
    // Constructor 1: Full parameterized constructor
    // This is called when we create a new Student object
    /**
     * Creates a new Student with all details
     * @param name - Student's name
     * @param rollNumber - Student's roll number
     * @param age - Student's age
     * @param grade - Student's grade percentage
     */
    public Student(String name, int rollNumber, int age, double grade) {
        // 'this' keyword refers to the current object
        // 'this.name' refers to instance variable
        // 'name' (parameter) refers to the constructor parameter
        this.name = name;
        this.rollNumber = rollNumber;
        this.age = age;
        this.grade = grade;
    }
    
    // Constructor 2: Constructor Overloading
    // Provides a simpler way to create Student with default grade
    /**
     * Creates a new Student with default grade of 0.0
     */
    public Student(String name, int rollNumber, int age) {
        this.name = name;
        this.rollNumber = rollNumber;
        this.age = age;
        this.grade = 0.0;  // Default grade
    }
    
    // Constructor 3: Default constructor
    /**
     * Creates a Student with all default values
     */
    public Student() {
        this.name = "Unknown";
        this.rollNumber = 0;
        this.age = 0;
        this.grade = 0.0;
    }
    
    // Getter Methods (to access private variables)
    // These follow the JavaBean naming convention
    
    public String getName() {
        return name;
    }
    
    public int getRollNumber() {
        return rollNumber;
    }
    
    public int getAge() {
        return age;
    }
    
    public double getGrade() {
        return grade;
    }
    
    // Setter Methods (to modify private variables)
    // Allows controlled modification of instance variables
    
    public void setName(String name) {
        this.name = name;
    }
    
    public void setAge(int age) {
        // Can add validation logic here
        if (age > 0 && age < 100) {
            this.age = age;
        } else {
            System.out.println("Invalid age! Age not updated.");
        }
    }
    
    public void setGrade(double grade) {
        if (grade >= 0 && grade <= 100) {
            this.grade = grade;
        } else {
            System.out.println("Invalid grade! Grade must be between 0 and 100.");
        }
    }
    
    // Business Logic Methods
    // These methods define the behavior of Student objects
    
    /**
     * Displays all information about the student
     */
    public void displayInfo() {
        String separator = "========================================";
        System.out.println("\n" + separator);
        System.out.println("STUDENT INFORMATION");
        System.out.println(separator);
        System.out.println("Name: " + name);
        System.out.println("Roll Number: " + rollNumber);
        System.out.println("Age: " + age);
        System.out.println("Grade: " + grade + "%");
        System.out.println("Status: " + (hasPassed() ? "PASSED ✓" : "FAILED ✗"));
        System.out.println(separator);
    }
    
    /**
     * Updates the student's grade
     * @param newGrade - The new grade to set
     */
    public void updateGrade(double newGrade) {
        if (newGrade >= 0 && newGrade <= 100) {
            System.out.println("\nUpdating grade for " + name);
            System.out.println("Old Grade: " + this.grade + "%");
            this.grade = newGrade;
            System.out.println("New Grade: " + this.grade + "%");
        } else {
            System.out.println("\nError: Grade must be between 0 and 100!");
        }
    }
    
    /**
     * Checks if the student has passed (grade >= 40)
     * @return true if passed, false otherwise
     */
    public boolean hasPassed() {
        return grade >= 40.0;
    }
    
    /**
     * Returns the letter grade based on percentage
     * @return Letter grade (A, B, C, D, F)
     */
    public String getLetterGrade() {
        if (grade >= 90) {
            return "A";
        } else if (grade >= 80) {
            return "B";
        } else if (grade >= 70) {
            return "C";
        } else if (grade >= 60) {
            return "D";
        } else if (grade >= 40) {
            return "E";
        } else {
            return "F";
        }
    }
    
    /**
     * Returns a string representation of the Student object
     * This method overrides Object's toString() method
     */
    @Override
    public String toString() {
        return "Student{" +
                "name='" + name + '\'' +
                ", rollNumber=" + rollNumber +
                ", age=" + age +
                ", grade=" + grade +
                ", letterGrade=" + getLetterGrade() +
                '}';
    }
    
    /**
     * Compares this student with another student based on grades
     * @param other - Another student to compare with
     * @return positive if this student has higher grade, negative if lower, 0 if equal
     */
    public int compareGrades(Student other) {
        return Double.compare(this.grade, other.grade);
    }
}
