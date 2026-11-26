import java.io.*;
import java.util.HashMap;
import java.util.Scanner;

/**
 * PS 6 - Vaccination Slot Booking
 * Console application using a manual circular queue (array of size 3),
 * a HashMap for booking lookup, and file handling (bookings.txt).
 */

// ---------- Booking: one person's booking ----------
class Booking {
    String id;
    String name;
    int age;

    Booking(String id, String name, int age) {
        this.id = id;
        this.name = name;
        this.age = age;
    }
}

// ---------- CircularQueue: fixed-size array queue ----------
class CircularQueue {
    private Booking[] slots;   // the array that holds bookings
    private int front;         // index of the first (oldest) booking
    private int rear;          // index of the last (newest) booking
    private int count;         // how many bookings are inside
    private int capacity;      // fixed size (3)

    CircularQueue(int capacity) {
        this.capacity = capacity;
        slots = new Booking[capacity];
        front = 0;
        rear = -1;
        count = 0;
    }

    boolean isEmpty() { return count == 0; }
    boolean isFull()  { return count == capacity; }
    int size()        { return count; }

    // Insert at the rear (wraps around using %)
    boolean enqueue(Booking b) {
        if (isFull()) return false;
        rear = (rear + 1) % capacity;
        slots[rear] = b;
        count++;
        return true;
    }

    // Remove from the front (wraps around using %)
    Booking dequeue() {
        if (isEmpty()) return null;
        Booking b = slots[front];
        slots[front] = null;
        front = (front + 1) % capacity;
        count--;
        return b;
    }

    // Get the booking at a position (0 = front) without removing it
    Booking getAt(int position) {
        if (position < 0 || position >= count) return null;
        return slots[(front + position) % capacity];
    }
}

// ---------- Main class ----------
public class VaccinationSlotBooking {

    static final int CAPACITY = 3;
    static final int MIN_AGE = 18;
    static final String FILE_NAME = "bookings.txt";

    static CircularQueue queue = new CircularQueue(CAPACITY);
    static HashMap<String, Booking> bookingMap = new HashMap<String, Booking>();
    static int nextIdNumber = 101;   // next booking ID is "V" + this number
    static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        loadFromFile();

        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("===== VACCINATION SLOT BOOKING SYSTEM =====");
            System.out.println();
            System.out.println("1. Book Vaccination Slot");
            System.out.println("2. Vaccinate Next Person");
            System.out.println("3. View Current Queue");
            System.out.println("4. Exit");
            System.out.println();
            System.out.print("Enter your choice: ");

            String input = readLine();
            if (input == null) {          // input ended (Ctrl+D / piped input)
                saveToFile();
                break;
            }

            switch (input.trim()) {
                case "1": bookSlot(); break;
                case "2": vaccinateNext(); break;
                case "3": viewQueue(); break;
                case "4":
                    saveToFile();
                    System.out.println("Bookings saved. Goodbye!");
                    running = false;
                    break;
                default:
                    System.out.println("Invalid choice. Please enter 1, 2, 3 or 4.");
            }
        }
        sc.close();
    }

    // Reads one line; returns null if there is no more input
    static String readLine() {
        if (sc.hasNextLine()) return sc.nextLine();
        return null;
    }

    // ---------- Feature 1: Book a slot ----------
    static void bookSlot() {
        System.out.print("Enter name: ");
        String name = readLine();
        System.out.print("Enter age: ");
        String ageText = readLine();

        if (name == null || ageText == null) {
            System.out.println("Invalid input. Booking not done.");
            return;
        }
        name = name.trim();

        // Validate age (must be a number and at least 18)
        int age;
        try {
            age = Integer.parseInt(ageText.trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid age. Booking not done.");
            return;
        }
        if (age < MIN_AGE) {
            System.out.println("Invalid age. Booking not done.");
            return;
        }

        // Validate name (letters and spaces only, not empty)
        if (!name.matches("[A-Za-z ]+")) {
            System.out.println("Invalid name. Use letters only. Booking not done.");
            return;
        }

        // Check if all slots are taken
        if (queue.isFull()) {
            System.out.println("Slots full. Please try the next session.");
            return;
        }

        // Create the booking
        String id = "V" + nextIdNumber;
        nextIdNumber++;
        Booking b = new Booking(id, name, age);
        queue.enqueue(b);
        bookingMap.put(id, b);

        System.out.println("Slot booked. ID: " + id + " | " + name + " | Position: " + queue.size());
    }

    // ---------- Feature 2: Vaccinate the next person ----------
    static void vaccinateNext() {
        if (queue.isEmpty()) {
            System.out.println("No one in the queue. Nobody to vaccinate.");
            return;
        }
        Booking b = queue.dequeue();
        bookingMap.remove(b.id);
        System.out.println(b.name + " vaccinated. Slot freed.");
        System.out.println("(Booking ID: " + b.id + ")");
    }

    // ---------- Feature 3: View the queue ----------
    static void viewQueue() {
        if (queue.isEmpty()) {
            System.out.println("The queue is empty. No current bookings.");
            return;
        }
        System.out.println("Current Vaccination Queue:");
        for (int i = 0; i < queue.size(); i++) {
            Booking b = queue.getAt(i);
            System.out.println((i + 1) + ". " + b.id + " | " + b.name);
        }
    }

    // ---------- Feature 4: Save to file ----------
    // File format:  line 1 -> NEXT_ID=<number>
    //               other lines -> id,name,age   (oldest booking first)
    static void saveToFile() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE_NAME))) {
            pw.println("NEXT_ID=" + nextIdNumber);
            for (int i = 0; i < queue.size(); i++) {
                Booking b = queue.getAt(i);
                pw.println(b.id + "," + b.name + "," + b.age);
            }
        } catch (IOException e) {
            System.out.println("Error: could not save bookings: " + e.getMessage());
        }
    }

    // ---------- Load from file at startup ----------
    static void loadFromFile() {
        File file = new File(FILE_NAME);
        if (!file.exists()) {
            return;   // first run: nothing to load
        }
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            int highestId = 100;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.startsWith("NEXT_ID=")) {
                    try {
                        nextIdNumber = Integer.parseInt(line.substring(8).trim());
                    } catch (NumberFormatException e) {
                        // ignore a bad counter; fixed below using highestId
                    }
                    continue;
                }

                String[] parts = line.split(",");
                if (parts.length != 3) continue;          // skip damaged line
                String id = parts[0].trim();
                String name = parts[1].trim();
                int age;
                try {
                    age = Integer.parseInt(parts[2].trim());
                } catch (NumberFormatException e) {
                    continue;
                }
                if (!id.matches("V\\d+")) continue;
                if (bookingMap.containsKey(id)) continue; // avoid duplicates
                if (queue.isFull()) break;

                Booking b = new Booking(id, name, age);
                queue.enqueue(b);
                bookingMap.put(id, b);

                int number = Integer.parseInt(id.substring(1));
                if (number > highestId) highestId = number;
            }
            // Make sure new IDs never reuse an existing one
            if (nextIdNumber <= highestId) nextIdNumber = highestId + 1;
            System.out.println("Loaded " + queue.size() + " booking(s) from " + FILE_NAME + ".");
        } catch (IOException e) {
            System.out.println("Error: could not read bookings file: " + e.getMessage());
        }
    }
}
