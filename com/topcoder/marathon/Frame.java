package com.topcoder.marathon;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Frame {
    private CachedGraphics2D graphics;
    private Map<Object, Object> infoMap = new HashMap<Object, Object>();
    private Map<Object, Boolean> infoChecked = new HashMap<Object, Boolean>();
    private Map<Object, Rectangle2D> infoRects = new HashMap<Object, Rectangle2D>();
    private List<Object> infoSequence = new ArrayList<Object>();

    public static Rectangle2D contentRect = new Rectangle2D.Double(0, 0, 100, 100);
    public Rectangle2D contentScreen = new Rectangle2D.Double();
    public static Font infoFontPlain, infoFontBold;
    public static RenderingHints hints;
    public static int border, infoFontWidth, infoFontHeight, infoColumns, infoLines;
    public static final double lineSpacing = 1.25;

    public Frame(CachedGraphics2D graphics, Map<Object, Object> infoMap, Map<Object, Boolean> infoChecked, Map<Object, Rectangle2D> infoRects, List<Object> infoSequence) {
        this.graphics = graphics;
        this.infoMap = infoMap;
        this.infoChecked = infoChecked;
        this.infoRects = infoRects;
        this.infoSequence = infoSequence;
    }

    public void render(Graphics2D g, int w, int h) {
        g.setColor(new Color(230, 230, 232));
        g.fillRect(0, 0, w, h);
        g.setRenderingHints(hints);

        if (infoColumns > 0 && infoFontWidth > 0) paintInfo(g, w);
        paintCenter(g, infoFontWidth == 0 ? w : w - infoFontWidth * infoColumns - border, h);
    }

    private void paintCenter(Graphics2D g, int w, int h) {
        int pw = w - 2 * border;
        int ph = h - 2 * border;
        if (pw <= 0 || ph <= 0) return;
        int px = border;
        int py = border;
        if (contentRect.getWidth() * ph > contentRect.getHeight() * pw) {
            ph = (int) (contentRect.getHeight() * pw / contentRect.getWidth());
        } else {
            int nw = (int) (contentRect.getWidth() * ph / contentRect.getHeight());
            px += (pw - nw) / 2;
            pw = nw;
        }
        AffineTransform nt = new AffineTransform();
        nt.translate(px, py);
        nt.scale(pw / contentRect.getWidth(), ph / contentRect.getHeight());
        nt.translate(-contentRect.getX(), -contentRect.getY());
        AffineTransform ct = g.getTransform();
        contentScreen.setRect(px, py, pw, ph);
        graphics.build(g, nt);
        g.setTransform(ct);
    }

    private void paintInfo(Graphics2D g, int w) {
        int x = w - infoFontWidth * infoColumns - border;
        int y = border;
        int maxKey = 0;
        g.setFont(infoFontBold);
        FontMetrics metrics = g.getFontMetrics();
        for (Object key : infoSequence) {
            if (key != null) {
                String s = "";
                if (key instanceof Color) {
                    s = "##";
                } else {
                    s = key.toString();
                }
                boolean hasValue = infoMap.get(key) != null;
                if (hasValue) s += ": ";
                if (infoChecked.get(key) != null) s += "##";
                Rectangle2D rect = metrics.getStringBounds(s, g);
                int width = (int) rect.getWidth();
                if (!hasValue) width /= 2;
                maxKey = Math.max(maxKey, width);
            }
        }

        int lineHeight = (int) (lineSpacing * infoFontHeight);
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(Color.black);
        for (Object key : infoSequence) {
            if (key != null) {
                Object value = infoMap.get(key);
                g.setFont(infoFontBold);
                int xc = 0;
                if (value == null) {
                    xc = drawString(g, key.toString(), x + maxKey, y, 0);
                } else {
                    if (key instanceof Color) {
                        xc = drawColor(g, (Color) key, x + maxKey, y);
                    } else {
                        xc = drawString(g, key + ": ", x + maxKey, y, -1);
                    }
                    g.setFont(infoFontPlain);
                    drawString(g, value.toString(), x + maxKey, y, 1);
                }
                Boolean checked = infoChecked.get(key);
                if (checked != null) drawChecked(g, key, checked, xc, y);
            }
            y += lineHeight;
        }
    }

    private int drawColor(Graphics2D g, Color color, int x, int y) {
        FontMetrics metrics = g.getFontMetrics();
        int size = metrics.getHeight() - metrics.getDescent();
        g.setColor(color);
        Rectangle2D rc = new Rectangle2D.Double(x - size - infoFontWidth, y + metrics.getDescent() / 2, size, size);
        g.fill(rc);
        g.setColor(Color.black);
        g.draw(rc);
        return (int) rc.getMinX();
    }

    private void drawChecked(Graphics2D g, Object key, boolean checked, int x, int y) {
        FontMetrics metrics = g.getFontMetrics();
        int size = metrics.getHeight() - metrics.getDescent();
        Rectangle2D rc = new Rectangle2D.Double(x - size - infoFontWidth, y + metrics.getDescent() / 2, size, size);
        g.setColor(Color.black);
        g.draw(rc);
        synchronized (infoRects) {
            infoRects.put(key, rc);
        }
        if (checked) {
            g.draw(new Line2D.Double(rc.getMinX(), rc.getMinY(), rc.getMaxX(), rc.getMaxY()));
            g.draw(new Line2D.Double(rc.getMinX(), rc.getMaxY(), rc.getMaxX(), rc.getMinY()));
        }
    }

    private int drawString(Graphics2D g, String s, int x, int y, int align) {
        FontMetrics metrics = g.getFontMetrics();
        Rectangle2D rect = metrics.getStringBounds(s, g);
        if (align < 0) x -= (int) rect.getWidth();
        else if (align == 0) x -= (int) rect.getWidth() / 2;
        g.drawString(s, x, y + metrics.getAscent());
        return x;
    }
}
