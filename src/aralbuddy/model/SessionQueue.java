package aralbuddy.model;

/** Teaching FIFO queue that presents pending tutor requests in submission order. */
public class SessionQueue {

    private SessionNode front;
    private SessionNode rear;

    /** Appends a request to the back of this independent queue. */
    public void enqueue(Session session) {
        SessionNode node = new SessionNode(session);
        if (rear == null) {
            front = node;
        } else {
            rear.next = node;
        }
        rear = node;
    }

    /** Removes and returns the oldest request, or null when the queue is empty. */
    public Session dequeue() {
        if (front == null) {
            return null;
        }

        Session session = front.session;
        front = front.next;
        if (front == null) {
            rear = null;
        }
        return session;
    }

    /** Finds a request by visiting each queued node without changing the queue. */
    public Session linearSearchBySessionId(int sessionId) {
        for (SessionNode current = front; current != null; current = current.next) {
            if (current.session.getId() == sessionId) {
                return current.session;
            }
        }
        return null;
    }

    /** Reports whether the queue has no pending requests. */
    public boolean isEmpty() {
        return front == null;
    }
}

class SessionNode {

    Session session;
    SessionNode next;

    SessionNode(Session session) {
        this.session = session;
    }
}
