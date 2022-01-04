package com.topcoder.marathon;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.*;

/**
 * Base class for Topcoder Marathon testers with visualization. 
 * Should be extended directly for problems with a visual representation, but no animation,
 * i.e. only a single (final) state is shown.
 * 
 * Updates: 
 *      2020/11/19 - Handle -windowPos and -screen parameters.
 *      2021/02/05 - Move mouse click events to another (not AWT) thread, to avoid painting/  
 *                   delay issues after an user action.
 *                 - Override paint() instead of paintComponent().
 *                 - Use mousePress() event instead of mountClick() for better responsiveness.
 *                 - Add -saveVis parameter to allow saving the visualizer content, after 
 *                   each update.
 *                 - Add -infoScale parameter to allow increase/decrease the font used in the 
 *                   info panel (right side of visualizer). The panel is not displayed if 
 *                   infoScale is 0 (which may be useful if the user wants to see only the 
 *                   main content, possibly together with -saveVis parameter). 
 *      2021/09/13 - Small change in the way the frame is created (waiting, instead of doing
 *                   it in the background).             
 */
public abstract class MarathonVis extends MarathonTester {
    protected final Object updateLock = new Object();
    protected JFrame frame;
    private boolean vis = true;
    private JPanel panel;
    private Map<Object, Object> infoMap = new HashMap<Object, Object>();
    private Map<Object, Boolean> infoChecked = new HashMap<Object, Boolean>();
    private Map<Object, Rectangle2D> infoRects = new HashMap<Object, Rectangle2D>();
    private List<Object> infoSequence = new ArrayList<Object>();
    private double size = -1;
    private Rectangle2D contentScreen = new Rectangle2D.Double();
    private long paintTime;
    private int paintCnt;
    private int saveVisSeq;
    private BufferedImage lastSavedImage;

    private ArrayList<Frame> frames = new ArrayList<>();
    private int currentFrame = -1;

    protected abstract void paintContent(Graphics2D g);

    static {
        System.setProperty("sun.java2d.uiScale", "1");
        System.setProperty("sun.java2d.dpiaware", "true");
    }

    public void setParameters(Parameters parameters) {
        super.setParameters(parameters);
        if (parameters.isDefined(Parameters.noVis)) {
            System.setProperty("java.awt.headless", "true");
            vis = false;
        }
        if (parameters.isDefined(Parameters.size)) size = parameters.getIntValue(Parameters.size);
    }

    protected final void setInfoMaxDimension(int infoColumns, int infoLines) {
        if (!vis) return;
        Frame.infoColumns = infoColumns;
        Frame.infoLines = infoLines;
    }

    protected final void setContentRect(double xLeft, double yTop, double xRight, double yBottom) {
        if (!vis) return;
        Frame.contentRect.setRect(xLeft, yTop, xRight - xLeft, yBottom - yTop);
    }

    protected final void setDefaultSize(int size) {
        if (this.size == -1) this.size = size;
    }

    protected final boolean hasVis() {
        return vis;
    }

    protected void update() {
        if (!vis) return;
        synchronized (updateLock) {
            if (frame == null) {
                String className = getClass().getName();
                Map<RenderingHints.Key, Object> hintsMap = new HashMap<RenderingHints.Key, Object>();
                hintsMap.put(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                hintsMap.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                hintsMap.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
                hintsMap.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
                if (parameters.isDefined(Parameters.noAntialiasing)) {
                    hintsMap.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                    hintsMap.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
                } else {
                    hintsMap.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    hintsMap.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                }
                Frame.hints = new RenderingHints(hintsMap);

                frame = new JFrame();
                frame.addWindowListener(new WindowAdapter() {
                    public void windowClosed(WindowEvent e) {
                        end();
                    }
                });

                JPanel mainPanel = new JPanel(new BorderLayout());
                JSlider slider = new JSlider(JSlider.HORIZONTAL, 0, 0, 0);
                slider.setMajorTickSpacing(10);
                slider.setMinorTickSpacing(1);
                slider.addChangeListener(changeEvent -> { currentFrame = slider.getValue(); panel.repaint(); });
                panel = new JPanel() {
                    private static final long serialVersionUID = -1008231133177413855L;

                    public void paint(Graphics g) {
                        long t = System.currentTimeMillis();
                        synchronized (updateLock) {
                            if (currentFrame >= frames.size()) {
                                currentFrame = frames.size();
                                CachedGraphics2D cached = new CachedGraphics2D((Graphics2D) g);
                                paintContent(cached);
                                Frame frame = new Frame(cached, infoMap, infoChecked, infoRects, infoSequence);
                                infoMap = new HashMap<>();
                                infoChecked = new HashMap<>();
                                infoRects = new HashMap<>();
                                infoSequence = new ArrayList<>();
                                frames.add(frame);
                                slider.setMaximum(frames.size() - 1);
                                slider.setValue(frames.size() - 1);
                            }
                            frames.get(Math.min(currentFrame, frames.size() - 1)).render((Graphics2D) g, getWidth(), getHeight());
                        }
                        paintTime += System.currentTimeMillis() - t;
                        paintCnt++;
                    }
                };
                mainPanel.add(panel, BorderLayout.CENTER);
                mainPanel.add(slider, BorderLayout.NORTH);

                panel.addMouseListener(new MouseAdapter() {
                    public void mousePressed(MouseEvent e) {
                        if (contentScreen != null && contentScreen.contains(e.getPoint())) {
                            if (contentScreen.getWidth() > 0 && contentScreen.getHeight() > 0) {
                                double x = (e.getX() - contentScreen.getX()) / contentScreen.getWidth() * Frame.contentRect.getWidth() + Frame.contentRect.getX();
                                double y = (e.getY() - contentScreen.getY()) / contentScreen.getHeight() * Frame.contentRect.getHeight() + Frame.contentRect.getY();
                                new Thread() {
                                    public void run() {
                                        contentClicked(x, y, e.getButton(), e.getClickCount());
                                    }
                                }.start();
                            }
                            return;
                        }
                        for (Object key : infoRects.keySet()) {
                            Rectangle2D rc = infoRects.get(key);
                            if (rc != null && rc.contains(e.getPoint())) {
                                Boolean checked = infoChecked.get(key);
                                if (checked != null) {
                                    infoChecked.put(key, !checked);
                                    new Thread() {
                                        public void run() {
                                            checkChanged(key, !checked);
                                        }
                                    }.start();
                                }
                                break;
                            }
                        }
                    }
                });

                final int resolution = Toolkit.getDefaultToolkit().getScreenResolution();
                int infoScale = 100;
                if (parameters.isDefined(Parameters.infoScale)) infoScale = parameters.getIntValue(Parameters.infoScale);
                if (infoScale < 0) infoScale = 0;
                else if (infoScale > 400) infoScale = 400;
                if (infoScale != 0) {
                    Frame.infoFontPlain = new Font(Font.SANS_SERIF, Font.PLAIN, resolution * infoScale / 800);
                    Frame.infoFontBold = new Font(Font.SANS_SERIF, Font.BOLD, Frame.infoFontPlain.getSize());
                }

                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        public void run() {
                            frame.setSize(1000, 800);
                            frame.setTitle(className + " - Seed: " + seed);
                            frame.setIconImage(getIcon());
                            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

                            frame.setContentPane(mainPanel);

                            if (Frame.infoFontPlain != null) {
                                FontRenderContext frc = new FontRenderContext(null, true, true);
                                Rectangle2D rc = Frame.infoFontBold.getStringBounds("0", frc);
                                Frame.infoFontWidth = (int) Math.ceil(rc.getWidth());
                                Frame.infoFontHeight = (int) Math.ceil(rc.getHeight());
                            }

                            Frame.border = resolution / 7;
                            showAndAdjustWindowBounds();
                        }
                    });
                } catch (Exception e) {
                }
            }
        }
        if (parameters.isDefined(Parameters.saveVis)) saveVis();
        currentFrame++;
        panel.repaint();
    }

    private void saveVis() {
        int w = panel.getWidth();
        if (w > 0) {
            int h = panel.getHeight();
            String s = parameters.getStringNull(Parameters.saveVis);
            File folder = new File(s == null ? "." : s);
            if (!folder.exists()) folder.mkdirs();
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_BGR);
            Graphics2D g = img.createGraphics();
            frames.get(currentFrame).render(g, w, h);
            g.dispose();
            try {
                synchronized (updateLock) {
                    boolean eq = false;
                    if (lastSavedImage != null && lastSavedImage.getWidth() == w && lastSavedImage.getHeight() == h) {
                        int[] curr = img.getRGB(0, 0, w, h, null, 0, w);
                        int[] prev = lastSavedImage.getRGB(0, 0, w, h, null, 0, w);
                        eq = curr.length == prev.length;
                        if (eq) {
                            OUT: for (int i = 0; i < 256; i++) {
                                for (int j = i; j < curr.length; j += 256) {
                                    if (curr[j] != prev[j]) {
                                        eq = false;
                                        break OUT;
                                    }
                                }
                            }
                        }
                    }
                    if (!eq) {
                        File file = new File(folder, String.format("%d-%05d.png", seed, ++saveVisSeq));
                        ImageIO.write(img, "png", file);
                        lastSavedImage = img;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void showAndAdjustWindowBounds() {
        Rectangle screenBounds = null;
        Insets screenInsets = null;
        int screen = 1;
        if (parameters.isDefined(Parameters.screen)) {
            try {
                screen = Integer.parseInt(parameters.getString(Parameters.screen));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            GraphicsDevice[] graphicsDevices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
            int numScreen = 0;
            for (GraphicsDevice gd : graphicsDevices) {
                numScreen++;
                if (numScreen == 1 || numScreen == screen) {
                    GraphicsConfiguration gc = gd.getDefaultConfiguration();
                    screenBounds = new Rectangle(gc.getBounds());
                    screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        int x = Integer.MIN_VALUE, y = Integer.MIN_VALUE, w = Integer.MIN_VALUE, h = Integer.MIN_VALUE;
        if (parameters.isDefined(Parameters.windowPosition)) {
            String[] v = parameters.getString(Parameters.windowPosition).split(",");
            boolean ok = v.length == 2 || v.length == 4;
            if (ok) {
                try {
                    x = Integer.parseInt(v[0]);
                    y = Integer.parseInt(v[1]);
                    if (screenBounds != null) {
                        x += screenBounds.x;
                        y += screenBounds.y;
                    }
                    if (screenInsets != null) {
                        x += screenInsets.left;
                        y += screenInsets.top;
                    }
                    if (v.length > 2) {
                        w = Integer.parseInt(v[2]);
                        h = Integer.parseInt(v[3]);
                        if (w > 0 && h > 0) {
                            frame.setVisible(true);
                            frame.setBounds(x, y, w, h);
                        } else {
                            ok = false;
                        }
                    }
                } catch (Exception e) {
                    ok = false;
                }
            }
            if (!ok) {
                System.err.println("Parameter -" + Parameters.windowPosition + " should be followed by 2 or 4 integers: x,y[,width,height] of desired window position.");
                System.exit(0);
            }
        }
        if (w == Integer.MIN_VALUE || h == Integer.MIN_VALUE) {
            frame.setVisible(true);
            if (x != Integer.MIN_VALUE && y != Integer.MIN_VALUE) {
                frame.setLocation(x, y);
            } else if (screenBounds != null) {
                int xf = screenBounds.x;
                int yf = screenBounds.y;
                if (screenInsets != null) {
                    xf += screenInsets.left;
                    yf += screenInsets.top;
                }
                frame.setLocation(xf, yf);
            } else {
                frame.setLocation(0, 0);
            }
            if (size <= 0 && screenBounds != null) {
                Rectangle bounds = new Rectangle(screenBounds);
                if (x != Integer.MIN_VALUE && y != Integer.MIN_VALUE) {
                    bounds.x = x;
                    bounds.y = y;
                }
                if (screenInsets != null) {
                    int dx = 0;
                    int dy = 0;
                    if (bounds.x - screenBounds.x < screenInsets.left) dx = screenInsets.left - bounds.x;
                    if (bounds.y - screenBounds.y < screenInsets.top) dy = screenInsets.top - bounds.y;
                    bounds.x += dx;
                    bounds.y += dy;
                    if (bounds.x >= screenBounds.x && bounds.x < screenBounds.x + screenBounds.width) {
                        bounds.width -= Math.max(0, bounds.x - screenBounds.x + bounds.width - (screenBounds.width - screenInsets.right));
                    }
                    if (bounds.y >= screenBounds.y && bounds.y < screenBounds.y + screenBounds.height) {
                        bounds.height -= Math.max(0, bounds.y - screenBounds.y + bounds.height - (screenBounds.height - screenInsets.bottom));
                    }
                }
                Insets fi = frame.getInsets();
                int fw = bounds.width - fi.left - fi.right;
                int fh = bounds.height - fi.top - fi.bottom;
                double sw = (fw - 3 * Frame.border - Frame.infoColumns * Frame.infoFontWidth) / Frame.contentRect.getWidth();
                double sh = (fh - 2 * Frame.border) / Frame.contentRect.getHeight();
                size = Math.min(sw, sh);
            }
            int width = 2 * Frame.border + (int) (Frame.contentRect.getWidth() * size);
            if (Frame.infoFontWidth > 0) width += Frame.border + Frame.infoColumns * Frame.infoFontWidth;
            int height = 2 * Frame.border + (int) Math.max(Frame.infoLines * Frame.infoFontHeight * Frame.lineSpacing, Frame.contentRect.getHeight() * size);
            panel.setPreferredSize(new Dimension(width, height));
            frame.pack();
        }
    }

    @SuppressWarnings("unused")
    protected void checkChanged(Object key, boolean newValue) {
        panel.repaint();
    }

    @SuppressWarnings("unused")
    protected void contentClicked(double x, double y, int mouseButton, int clickCount) {
    }

    protected void end() {
        if (ending) return;
        if (paintCnt > 0 && parameters.isDefined(Parameters.paintInfo)) {
            System.out.println("    Paint Count: " + paintCnt);
            System.out.println("Paint Avg. Time: " + paintTime / paintCnt + " ms");
        }
        super.end();
    }

    protected final void addInfo(Object key, Object value) {
        if (!vis) return;
        if (!infoMap.containsKey(key)) infoSequence.add(key);
        infoMap.put(key, value);
    }

    protected final void addInfo(Object key) {
        if (!vis) return;
        if (!infoMap.containsKey(key)) infoSequence.add(key);
        infoMap.put(key, null);
    }

    protected final void addInfo(Object key, Object value, boolean checked) {
        if (!vis) return;
        if (!infoMap.containsKey(key)) infoSequence.add(key);
        infoMap.put(key, value);
        infoChecked.put(key, checked);
    }

    protected final void addInfo(Object key, boolean checked) {
        if (!vis) return;
        if (!infoMap.containsKey(key)) infoSequence.add(key);
        infoMap.put(key, null);
        infoChecked.put(key, checked);
    }

    protected final void addInfoBreak() {
        if (!vis) return;
        infoSequence.add(null);
    }

    protected final boolean isInfoChecked(Object key) {
        Boolean checked = infoChecked.get(key);
        if (checked != null) return checked.booleanValue();
        return false;
    }

    protected final Rectangle2D getPaintRect() {
        return Frame.contentRect;
    }

    private BufferedImage getIcon() {
        int size = 256;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHints(Frame.hints);
        AffineTransform nt = new AffineTransform();
        nt.scale(size, size);
        g.setTransform(nt);
        g.setStroke(new BasicStroke(0.06f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        GradientPaint gradient = new GradientPaint(0.5f, 0, new Color(150, 75, 200), 0.5f, 1, new Color(90, 160, 230));
        g.setPaint(gradient);
        Ellipse2D e0 = new Ellipse2D.Double(0, 0, 1, 1);
        g.fill(e0);
        Ellipse2D e1 = new Ellipse2D.Double(0.05, 0.45, 0.2, 0.2);
        Ellipse2D e2 = new Ellipse2D.Double(0.30, 0.05, 0.2, 0.2);
        Ellipse2D e3 = new Ellipse2D.Double(0.75, 0.25, 0.2, 0.2);
        Ellipse2D e4 = new Ellipse2D.Double(0.70, 0.60, 0.2, 0.2);
        Ellipse2D e5 = new Ellipse2D.Double(0.35, 0.75, 0.2, 0.2);
        g.setColor(Color.white);
        g.setStroke(new BasicStroke(0.06f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(e1.getCenterX(), e1.getCenterY(), e2.getCenterX(), e2.getCenterY()));
        g.draw(new Line2D.Double(e1.getCenterX(), e1.getCenterY(), e3.getCenterX(), e3.getCenterY()));
        g.draw(new Line2D.Double(e4.getCenterX(), e4.getCenterY(), e3.getCenterX(), e3.getCenterY()));
        g.draw(new Line2D.Double(e4.getCenterX(), e4.getCenterY(), e5.getCenterX(), e5.getCenterY()));
        g.draw(new Line2D.Double(e2.getCenterX(), e2.getCenterY(), e5.getCenterX(), e5.getCenterY()));
        g.draw(new Line2D.Double(e2.getCenterX(), e2.getCenterY(), e4.getCenterX(), e4.getCenterY()));
        g.setPaint(gradient);
        g.fill(e1);
        g.fill(e2);
        g.fill(e3);
        g.fill(e4);
        g.fill(e5);
        g.setColor(Color.white);
        g.draw(e1);
        g.draw(e2);
        g.draw(e3);
        g.draw(e4);
        g.draw(e5);
        g.dispose();
        float[] blurKernel = {0.1f,0.1f,0.1f,0.1f,0.2f,0.1f,0.1f,0.1f,0.1f};
        BufferedImageOp blurFilter = new ConvolveOp(new Kernel(3, 3, blurKernel), ConvolveOp.EDGE_NO_OP, Frame.hints);
        blurFilter.filter(img, null);
        return img;
    }
}
