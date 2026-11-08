package aralbuddy.model;

import java.util.LinkedList;

/** Teaching data structure that keeps sessions in insertion order with unique IDs. */
public class SinglyLinkedList {

    private Node head;
    private Node tail;
    private int size;

    /** Appends a non-null session unless its ID is already in this list. */
    public boolean add(Session session) {
        if (session == null || searchById(session.getId()) != null) {
            return false;
        }

        Node node = new Node(session);
        if (head == null) {
            head = node;
        } else {
            tail.next = node;
        }
        tail = node;
        size++;
        return true;
    }

    /** Returns a list copy in insertion order without changing the linked nodes. */
    public LinkedList<Session> display() {
        LinkedList<Session> sessions = new LinkedList<Session>();
        Node current = head;
        while (current != null) {
            sessions.add(current.session);
            current = current.next;
        }
        return sessions;
    }

    /** Finds a session by its stable ID, or returns null. */
    public Session searchById(int sessionId) {
        Node current = head;
        while (current != null) {
            if (current.session.getId() == sessionId) {
                return current.session;
            }
            current = current.next;
        }
        return null;
    }

    /** Removes a session by ID and maintains the first and last links. */
    public boolean deleteById(int sessionId) {
        Node previous = null;
        Node current = head;

        while (current != null) {
            if (current.session.getId() == sessionId) {
                if (previous == null) {
                    head = current.next;
                } else {
                    previous.next = current.next;
                }
                if (current == tail) {
                    tail = previous;
                }
                size--;
                return true;
            }
            previous = current;
            current = current.next;
        }
        return false;
    }

    /** Removes all linked nodes and resets the list size. */
    public void clear() {
        head = null;
        tail = null;
        size = 0;
    }

    /** Returns the number of linked session records. */
    public int size() {
        return size;
    }

    private static class Node {

        private final Session session;
        private Node next;

        private Node(Session session) {
            this.session = session;
        }
    }
}
