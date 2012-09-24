package robombs.game.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.SortedMap;
import java.util.TreeMap;

import com.threed.jpct.FrameBuffer;

/**
 * <p>creates GL renderable (blittable) font out of given AWT font.
 * a jPCT texture is created and added to TextureManager on the fly.</p>
 *
 * <p>in contrast with its name, this class can be used for software renderer too.
 * but to tell the truth, i would stick to Java2D for software renderer ;)</p>
 *
 * this class uses {@link TexturePack} behind the scenes.
 *
 * @see TexturePack
 *
 * @author hakan eryargi (r a f t)
 */
public class GLFont {
	/** standard characters */
	public static final String ENGLISH = " abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ`1234567890-=~!@#$%^&*()_+[]{}\\|:;\"'<>,.?/";

	/** German specific characters */
	public static final String GERMAN = new String(new char[] {
			'\u00c4', '\u00D6', '\u00DC', '\u00E4', '\u00F6', '\u00FC', '\u00DF' });

	/** French specific characters */
	public static final String FRENCH = new String(new char[] {
			'\u00C0', '\u00C2', '\u00C6', '\u00C8', '\u00C9', '\u00CA', '\u00CB',
			'\u00CE', '\u00CF', '\u00D4', '\u0152', '\u00D9', '\u00DB', '\u00DC',
			'\u0178', '\u00C7', '\u00E0', '\u00E2', '\u00E6', '\u00E8', '\u00E9',
			'\u00EA', '\u00EB', '\u00EE', '\u00EF', '\u00F4', '\u0153', '\u00F9',
			'\u00FB', '\u00FC', '\u00FF', '\u00E7' });

	/** Turkish specific characters */
	public static final String TURKISH = new String(new char[] {
		    '\u00e7', '\u00c7', '\u011f', '\u011e', '\u0131', '\u0130',
		    '\u00f6', '\u00d6', '\u015f', '\u015e', '\u00fc', '\u00dc' });

	/**
	 * same as getGLFont(font, ENGLISH)
	 * @see #getGLFont(Font, String)
	 * */
	public static GLFont getGLFont(Font font) {
		return getGLFont(font, ENGLISH);
	}

	/** returns cached font or creates a new one caches and returns it.
	 * note: cache mechanism doesnt take alphabet into account. */
	public static GLFont getGLFont(Font font, String alphabet) {
		String key = font.getName() + "/" + font.getSize() + "/" + font.getStyle();

		synchronized (FONTS) {
			GLFont glFont = FONTS.get(key);
			if (glFont == null) {
				glFont = new GLFont(font, alphabet);
				FONTS.put(key, glFont);
			}
			return glFont;
		}
	}
	/** our font cache */
	private static SortedMap<String, GLFont> FONTS = new TreeMap<String, GLFont>();

	/** the awt font */
	public final Font font;
	/** characters this GLFont is created for */
	public final String alphabet;
	/** regular font height. note some special characters may not fit into this height.
	 * see {@link FontMetrics} for a discussion */
	public final int fontHeight;
	private final int baseline;

	private final int[] charWidths;
	private final Dimension stringBounds = new Dimension();

	private final TexturePack pack = new TexturePack();

	/**
	 * creates a GLFont for given awt Font consists of default characters.
	 * @see #ENGLISH
	 */
	public GLFont(Font font) {
		this(font, ENGLISH);
	}

	/**
	 * creates a GLFont for given awt Font consists of characters in given alphabet
	 * @param font the awt font
	 * @param alphabet characters of our alphabet
	 */
	public GLFont(Font font, String alphabet) {
		this.font = font;
		this.alphabet = eliminateDuplicates(alphabet);
		this.charWidths = new int[alphabet.length()];

		Graphics2D g2d = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

		FontMetrics fontMetrics = g2d.getFontMetrics(font);

		this.fontHeight = fontMetrics.getHeight();
		this.baseline = fontMetrics.getMaxAscent();
		int height = fontMetrics.getMaxAscent() + fontMetrics.getMaxDescent();

		for (int i = 0; i < alphabet.length(); i++) {
			String c = alphabet.substring(i, i + 1);
			Rectangle2D bounds = fontMetrics.getStringBounds(c, g2d);
			int width = (int) bounds.getWidth();
			charWidths[i] = width;

			BufferedImage charImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			Graphics2D charGraphics = charImage.createGraphics();
			charGraphics.setRenderingHints(g2d.getRenderingHints());

			charGraphics.setFont(font);
			charGraphics.setColor(Color.WHITE);
			charGraphics.drawString(c, 0, baseline);

			charGraphics.dispose();
			pack.addImage(charImage);
		}
		pack.pack(TexturePack.ALPHA_USE);
	}

	private String eliminateDuplicates(String s) {
		StringBuilder sb = new StringBuilder(s);

		for (int i = 0; i < sb.length(); i++) {
			String c = sb.substring(i, i + 1);
			int next = -1;
			while ((next = sb.indexOf(c, i + 1)) != -1) {
				sb.deleteCharAt(next);
			}
		}
		return sb.toString();
	}

	/**
	 * returns how much area given string occupies. note this method always
	 * returns same Dimension instance
	 */
	public Dimension getStringBounds(String s) {
		int width = 0;

		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			int index = alphabet.indexOf(c);
			if (index == -1)
				index = alphabet.indexOf('?');
			if (index != -1) {
				width += charWidths[index];
			}
		}
		stringBounds.setSize(width, fontHeight);
		return stringBounds;
	}

	/**
	 * blits given string to frame buffer. works very similar to
	 * awt.Graphics#drawString(..) that is: x coordinate is left most point in
	 * string, y is baseline
	 *
	 * @param buffer
	 *            buffer to blit into
	 * @param s
	 *            string to blit
	 * @param x
	 *            leftmost point
	 * @param transparency
	 *            transparency value, make sure >= 0
	 * @param color
	 *            text color
	 * @param y
	 *            baseline
	 */
	public void blitString(FrameBuffer buffer, String s, int x, int y, int transparency, Color color) {
		y -= baseline;

		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			int index = alphabet.indexOf(c);
			if (index == -1)
				index = alphabet.indexOf('?');
			if (index != -1) {
				Dimension size = pack.blit(buffer, index, x, y, transparency, false, color);
				x += size.width;
			}
		}
	}

}
