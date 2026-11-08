package aralbuddy.view;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollBarUI;

/** Draws quiet, high-contrast scrollbars using the standard Swing painting API. */
public final class QuietScrollBarUI extends BasicScrollBarUI {
    /** Creates an independent UI delegate for each scrollbar. */
    public static ComponentUI createUI(JComponent component) { return new QuietScrollBarUI(); }

    @Override protected JButton createDecreaseButton(int orientation) { return hiddenArrow(); }
    @Override protected JButton createIncreaseButton(int orientation) { return hiddenArrow(); }
    @Override protected void paintTrack(Graphics graphics, JComponent component, Rectangle bounds) {
        graphics.setColor(Theme.BACKGROUND); graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
    }
    @Override protected void paintThumb(Graphics graphics, JComponent component, Rectangle bounds) {
        if (bounds.isEmpty() || !scrollbar.isEnabled()) return;
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(isThumbRollover() ? Theme.TEAL : new Color(161, 183, 177));
        g.fillRoundRect(bounds.x + 2, bounds.y + 2, Math.max(3, bounds.width - 4), Math.max(3, bounds.height - 4), 6, 6); g.dispose();
    }
    private JButton hiddenArrow() {
        JButton button = new JButton(); button.setPreferredSize(new Dimension(0, 0)); button.setMinimumSize(new Dimension(0, 0)); button.setMaximumSize(new Dimension(0, 0));
        return button;
    }
}
