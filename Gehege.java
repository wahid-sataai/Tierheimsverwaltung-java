package org.example;

public class Gehege {

        private int id;
        private String name;
        private int capacity;
        private int occupancy;

        public Gehege(int id, String name, int capacity, int occupancy) {
            this.id = id;
            this.name = name;
            this.capacity = capacity;
            this.occupancy = occupancy;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public int getCapacity() { return capacity; }
        public int getOccupancy() { return occupancy; }

        @Override
        public String toString() {
            return String.format("[%d] %s | Occupied: %d/%d", id, name, occupancy, capacity);
        }

}
