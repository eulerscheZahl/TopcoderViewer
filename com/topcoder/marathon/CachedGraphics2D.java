package com.topcoder.marathon;

import sun.java2d.SunGraphics2D;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.ArrayList;
import java.util.Map;

public class CachedGraphics2D extends Graphics2D {
    private Graphics2D graphics;
    private AffineTransform initialTransform;
    private ArrayList<CacheEntity> cache = new ArrayList<>();

    public CachedGraphics2D(Graphics2D graphics) {
        this.graphics = graphics;
        initialTransform = graphics.getTransform();
    }

    public void build(Graphics2D g, AffineTransform transform) {
        if (transform == null) g.setTransform(initialTransform);
        else g.setTransform(transform);
        for (CacheEntity c : cache) c.apply(g);
    }

    abstract class CacheEntity {
        public abstract void apply(Graphics2D g);
    }

    abstract class TwoPointEntity extends CacheEntity {
        protected int x1;
        protected int y1;
        protected int x2;
        protected int y2;

        public TwoPointEntity(int x1, int y1, int x2, int y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }
    }

    class DrawLineEntity extends TwoPointEntity {
        public DrawLineEntity(int x1, int y1, int x2, int y2) {
            super(x1, y1, x2, y2);
        }

        @Override
        public void apply(Graphics2D g) {
            g.drawLine(x1, y1, x2, y2);
        }
    }

    class FillRectEntity extends TwoPointEntity {
        public FillRectEntity(int x1, int y1, int x2, int y2) {
            super(x1, y1, x2, y2);
        }

        @Override
        public void apply(Graphics2D g) {
            g.fillRect(x1, y1, x2, y2);
        }
    }

    class DrawRectEntity extends TwoPointEntity {
        public DrawRectEntity(int x1, int y1, int x2, int y2) {
            super(x1, y1, x2, y2);
        }

        @Override
        public void apply(Graphics2D g) {
            g.drawRect(x1, y1, x2, y2);
        }
    }

    class ColorEntity extends CacheEntity {
        private Color color;

        public ColorEntity(Color color) {
            this.color = color;
        }

        @Override
        public void apply(Graphics2D g) {
            g.setColor(color);
        }
    }

    class StrokeEntity extends CacheEntity {
        private Stroke stroke;

        public StrokeEntity(Stroke stroke) {
            this.stroke = stroke;
        }

        @Override
        public void apply(Graphics2D g) {
            g.setStroke(stroke);
        }
    }

    class RenderHintsEntity extends CacheEntity {
        private Map<?, ?> map;

        public RenderHintsEntity(Map<?, ?> map) {
            this.map = map;
        }

        @Override
        public void apply(Graphics2D g) {
            g.setRenderingHints(map);
        }
    }

    class FontEntity extends CacheEntity {
        private Font font;

        public FontEntity(Font font) {
            this.font = font;
        }

        @Override
        public void apply(Graphics2D g) {
            g.setFont(font);
        }
    }

    class StringEntity extends CacheEntity {
        private float x;
        private float y;
        private String text;

        public StringEntity(String text, float x, float y) {
            this.text = text;
            this.x = x;
            this.y = y;
        }

        @Override
        public void apply(Graphics2D g) {
            g.drawString(text, x, y);
        }
    }

    class ImageEntity extends TwoPointEntity {
        private Image image;
        private ImageObserver imageObserver;

        public ImageEntity(Image image, int x1, int y1, int x2, int y2, ImageObserver imageObserver) {
            super(x1, y1, x2, y2);
            this.image = image;
            this.imageObserver = imageObserver;
        }

        @Override
        public void apply(Graphics2D g) {
            g.drawImage(image, x1, y1, x2, y2, imageObserver);
        }
    }

    class SetTransformEntity extends CacheEntity {
        private AffineTransform transform;

        public SetTransformEntity(AffineTransform transform) {
            this.transform = transform;
        }

        @Override
        public void apply(Graphics2D g) {
            g.setTransform(transform);
        }
    }

    @Override
    public void draw(Shape shape) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public boolean drawImage(Image image, AffineTransform affineTransform, ImageObserver imageObserver) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void drawImage(BufferedImage bufferedImage, BufferedImageOp bufferedImageOp, int x, int y) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void drawRenderedImage(RenderedImage renderedImage, AffineTransform affineTransform) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void drawRenderableImage(RenderableImage renderableImage, AffineTransform affineTransform) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void drawString(String s, int x, int y) {
        drawString(s, (float) x, (float) y);
    }

    @Override
    public void drawString(String s, float x, float y) {
        cache.add(new StringEntity(s, x, y));
    }

    @Override
    public void drawString(AttributedCharacterIterator attributedCharacterIterator, int i, int i1) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public boolean drawImage(Image image, int i, int i1, ImageObserver imageObserver) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public boolean drawImage(Image image, int x1, int y1, int x2, int y2, ImageObserver imageObserver) {
        cache.add(new ImageEntity(image, x1, y1, x2, y2, imageObserver));
        return true;
    }

    @Override
    public boolean drawImage(Image image, int i, int i1, Color color, ImageObserver imageObserver) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public boolean drawImage(Image image, int i, int i1, int i2, int i3, Color color, ImageObserver imageObserver) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public boolean drawImage(Image image, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7, ImageObserver imageObserver) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public boolean drawImage(Image image, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7, Color color, ImageObserver imageObserver) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void dispose() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void drawString(AttributedCharacterIterator attributedCharacterIterator, float v, float v1) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void drawGlyphVector(GlyphVector glyphVector, float v, float v1) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void fill(Shape shape) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public boolean hit(Rectangle rectangle, Shape shape, boolean b) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public GraphicsConfiguration getDeviceConfiguration() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void setComposite(Composite composite) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void setPaint(Paint paint) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void setStroke(Stroke stroke) {
        cache.add(new StrokeEntity(stroke));
    }

    @Override
    public void setRenderingHint(RenderingHints.Key key, Object o) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public Object getRenderingHint(RenderingHints.Key key) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void setRenderingHints(Map<?, ?> map) {
        cache.add(new RenderHintsEntity(map));
    }

    @Override
    public void addRenderingHints(Map<?, ?> map) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public RenderingHints getRenderingHints() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public Graphics create() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void translate(int i, int i1) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public Color getColor() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void setColor(Color color) {
        cache.add(new ColorEntity(color));
    }

    @Override
    public void setPaintMode() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void setXORMode(Color color) {
        throw new RuntimeException("not implemented");
    }

    private Font font;

    @Override
    public Font getFont() {
        return font;
    }

    @Override
    public void setFont(Font font) {
        cache.add(new FontEntity(font));
        this.font = font;
    }

    @Override
    public FontMetrics getFontMetrics(Font font) {
        return graphics.getFontMetrics(font);
    }

    @Override
    public Rectangle getClipBounds() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void clipRect(int i, int i1, int i2, int i3) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void setClip(int i, int i1, int i2, int i3) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public Shape getClip() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void setClip(Shape shape) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void copyArea(int i, int i1, int i2, int i3, int i4, int i5) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        cache.add(new DrawLineEntity(x1, y1, x2, y2));
    }

    @Override
    public void fillRect(int x1, int y1, int x2, int y2) {
        cache.add(new FillRectEntity(x1, y1, x2, y2));
    }

    @Override
    public void drawRect(int x1, int y1, int x2, int y2) {
        cache.add(new DrawRectEntity(x1, y1, x2, y2));
    }

    @Override
    public void clearRect(int i, int i1, int i2, int i3) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void drawRoundRect(int i, int i1, int i2, int i3, int i4, int i5) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void fillRoundRect(int i, int i1, int i2, int i3, int i4, int i5) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void drawOval(int i, int i1, int i2, int i3) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void fillOval(int i, int i1, int i2, int i3) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void drawArc(int i, int i1, int i2, int i3, int i4, int i5) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void fillArc(int i, int i1, int i2, int i3, int i4, int i5) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void drawPolyline(int[] ints, int[] ints1, int i) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void drawPolygon(int[] ints, int[] ints1, int i) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void fillPolygon(int[] ints, int[] ints1, int i) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void translate(double v, double v1) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void rotate(double v) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void rotate(double v, double v1, double v2) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void scale(double v, double v1) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void shear(double v, double v1) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void transform(AffineTransform affineTransform) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void setTransform(AffineTransform affineTransform) {
        graphics.setTransform(affineTransform);
        cache.add(new SetTransformEntity(affineTransform));
    }

    @Override
    public AffineTransform getTransform() {
        return graphics.getTransform();
    }

    @Override
    public Paint getPaint() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public Composite getComposite() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void setBackground(Color color) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public Color getBackground() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public Stroke getStroke() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void clip(Shape shape) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public FontRenderContext getFontRenderContext() {
        return graphics.getFontRenderContext();
    }
}
