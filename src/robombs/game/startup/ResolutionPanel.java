package robombs.game.startup;

import com.threed.jpct.*;
import com.threed.jpct.util.GLSLShader;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.io.*;
import org.lwjgl.opengl.*;
import robombs.game.*;
import robombs.game.talkback.*;

public class ResolutionPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	private static final float ZBIAS_OFFSET = 0.0f;
	private static final float ZBIAS_OFFSET_ATI_1XX0 = 0;// -0.75f;
	private static final float ZBIAS_OFFSET_ATI_YX0 = 0;// -0.5f; // Guessed!

	private JComboBox modes = null;
	private JComboBox rez = null;
	private JButton click = null;
	private Map<String, List<VideoMode>> resolutions = null;
	private VideoMode vm = null;
	private boolean optionsVisible = false;
	private JPanel subPanel = null;
	private JSlider shadows = null;
	private JSlider mouse = null;
	private JSlider aa = null;
	private JCheckBox fullscreen = null;
	private JCheckBox shaders = null;
	private JCheckBox highDetail = null;
	private JCheckBox skyBox = null;
	private JCheckBox shadowFilter = null;
	private Color gray = new Color(230, 230, 230);
	private Properties probs = null;

	private String vendor = "unknown";
	private String renderer = "unknown";
	private boolean canDoShadows = true;
	private boolean canDoBloom = true;
	private boolean canDoAA = true;
	private boolean canDoFBO = true;
	private boolean canDoShaders = true;

	private int guessedAAValue = 0;
	private int guessedShadowValue = 3;

	private String hw = "unknown";

	public VideoMode getMode() {
		return vm;
	}

	public boolean isFullscreen() {
		return fullscreen.isSelected();
	}

	public int getShadowQuality() {
		return shadows.getValue();
	}

	public int getMouseSpeed() {
		return mouse.getValue();
	}

	public boolean getShadowFiltering() {
		return shadowFilter.isSelected();
	}

	public int getAntiAliasingMode() {
		return aa.getValue();
	}

	public void setActionListener(final ActionListener al) {
		click.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				List<VideoMode> lst = resolutions.get(modes.getSelectedItem());
				int rezzSel = ((Integer) rez.getSelectedItem()).intValue();
				for (VideoMode v : lst) {
					int rezz = v.refresh;
					if (rezz <= rezzSel) {
						vm = v;
					}
				}
				al.actionPerformed(event);
			}
		});
	}

	private void collectFeatures() {
		JFrame jd = new JFrame();
		jd.setAlwaysOnTop(true);
		jd.setPreferredSize(new Dimension(400, 250));
		jd.setSize(new Dimension(400, 250));
		jd.setMinimumSize(new Dimension(400, 250));
		jd.setUndecorated(true);
		jd.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 105));
		JLabel text = new JLabel("Detecting hardware...");
		text.setFont(new Font("Arial", Font.ITALIC, 30));
		jd.add(text, JLabel.CENTER);
		jd.setLocationRelativeTo(null);
		jd.setVisible(true);
		try {
			org.lwjgl.opengl.DisplayMode dm = new org.lwjgl.opengl.DisplayMode(1, 1);
			Display.setDisplayMode(dm);
			Display.create(new PixelFormat());
			vendor = GL11.glGetString(GL11.GL_VENDOR);
			renderer = GL11.glGetString(GL11.GL_RENDERER);
			canDoShadows = GLContext.getCapabilities().GL_ARB_shadow;
			canDoBloom = GLContext.getCapabilities().GL_EXT_texture_rectangle;
			canDoAA = GLContext.getCapabilities().GL_ARB_multisample;
			canDoFBO = GLContext.getCapabilities().GL_EXT_framebuffer_object;

			int shaderModel = GLSLShader.guessShaderModel();

			canDoShaders = shaderModel != 9999 && shaderModel > 1;

			Display.destroy();
			String adapter = vendor + "-" + renderer + "/" + canDoShadows + "/" + canDoBloom + "/" + canDoAA + "/" + canDoFBO + "/" + canDoShaders;
			System.out.println(adapter);
			Globals.graphicsAdapter = adapter;
			Thread.sleep(500);

			String vl = vendor.toLowerCase();
			String rl = renderer.toLowerCase();
			hw = vl + "/" + rl;

			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < rl.length(); i++) {
				if (Character.isDigit(rl.charAt(i))) {
					sb.append(rl.charAt(i));
				} else {
					if (sb.length() > 2) {
						break;
					}
				}
			}
			System.out.println("[" + sb + "]");
			if (sb.length() == 0) {
				sb.append(0);
			}
			Integer number = Integer.parseInt(sb.toString());

			boolean isAGP = hw.indexOf("agp") != -1;

			Config.glShadowZBias = ZBIAS_OFFSET; // Default for all...
			Config.glVertexArrays = true;

			if (hw.indexOf("intel") != -1 || hw.indexOf("s3") != -1 || hw.indexOf("sis") != -1) {
				// Oh my god...it's intel or s3 or sis. Disable everything and
				// it *might* work well...
				guessedAAValue = 0;
				guessedShadowValue = 0;
				if (hw.indexOf("sis") != -1) {
					// If it's SIS, it may has problems with vertex
					// arrays...disable them and use strips instead.
					Config.glVertexArrays = false;
					Config.glTriangleStrips = true;
				}
			} else {
				if (hw.indexOf("ati") != -1 && hw.indexOf("nvidia") == -1) {

					// "ati" is part of the word "corporation"...too bad...
					if (number < 9500 && number > 7000) {
						// Old cards...
						guessedAAValue = 0;
						guessedShadowValue = 0;
					}
					if (number >= 9500 && number <= 9900) {
						// At least DX9 cards...
						guessedAAValue = 0;
						guessedShadowValue = 0;
					}
					if ((number >= 300 && number <= 600) || (number > 1000 && number < 1800)) {
						// Newer lowend DX9 cards...
						guessedAAValue = 0;
						guessedShadowValue = 0;
						if (number > 1000) {
							Config.glShadowZBias = ZBIAS_OFFSET_ATI_1XX0; // But
							// ATI...
						} else {
							Config.glShadowZBias = ZBIAS_OFFSET_ATI_YX0;
						}
					}
					if (number > 600 && number <= 900) {
						// Better DX9 cards...
						guessedAAValue = 2;
						guessedShadowValue = 0;
						Config.glShadowZBias = ZBIAS_OFFSET_ATI_YX0;
					}
					if (number >= 1800 && number <= 1950) {
						// Much better DX9 cards...
						Config.glShadowZBias = ZBIAS_OFFSET_ATI_1XX0; // But
						// ATI...
						guessedAAValue = 2;
						guessedShadowValue = 3;
					}
					if (number >= 2000 && number < 7000) {
						// DX10...
						guessedAAValue = 2;
						guessedShadowValue = 3;

						if (number == 2900 || number > 3800) {
							// Better DX10
							guessedAAValue = 4;
							guessedShadowValue = 4;
						}
					}
				} else {
					if (hw.indexOf("nvidia") != -1) {
						boolean isQuadro = hw.indexOf("quadro") != -1;
						if ((number < 5000 && number > 1000) || number < 200) {
							// Old cards...
							guessedAAValue = 0;
							guessedShadowValue = 0;
						}
						if (number >= 5000 && number < 6000) {
							// The first DX9 generation...
							guessedAAValue = 0;
							guessedShadowValue = 0;
						}
						if (number >= 6000 && number < 7000) {
							// The next DX9 generation...
							guessedAAValue = 0;
							guessedShadowValue = 2;
							if (number <= 6500) {
								guessedShadowValue = 0;
							}
						}
						if (number >= 7000 && number < 8000) {
							// The final DX9 generation...
							guessedAAValue = 2;
							guessedShadowValue = 3;
							if (number <= 7400) {
								guessedAAValue = 0;
								guessedShadowValue = 2;
							}
						}
						if (number >= 8000 || (number <= 1000 && number >= 200)) {
							// At least DX10...
							guessedAAValue = 4;
							guessedShadowValue = 4;

							if (number >= 8000 && number < 8600) {
								// Not so good DX10...
								guessedAAValue = 0;
								guessedShadowValue = 3;
							}
						}
						if (isQuadro) {
							// A Quadro (usually some business crap, but at
							// least fast enough for normal shadow quality in
							// most cases)
							guessedAAValue = 0;
							guessedShadowValue = 3;
						}
					}
				}
				if (!canDoFBO) {
					// No FBOs? Shadows are still possible but not recommend.
					guessedShadowValue = 0;
				}
			}

			if (isAGP) {
				// Bei AGP etwa zurückdrehen.
				guessedAAValue = 0;
				if (guessedShadowValue >= 3) {
					guessedShadowValue--;
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Unable to detect hardware!");
		}

		jd.setVisible(false);
		jd.dispose();

		if (hw.indexOf("mesa") != -1 && (hw.indexOf("software") != -1 || hw.indexOf("glx") != -1)) {
			JOptionPane
					.showMessageDialog(
							this,
							"Your system seems to use the MESA OpenGL driver,\nwhich means that the game will most likely run in\nsoftware emulation mode. This will be very slow!\n\nPlease consider to upgrade your video drivers!");
		}
	}

	@SuppressWarnings("unchecked")
	public ResolutionPanel(JFrame parentFrame) {

		collectFeatures();

		setBackground(java.awt.Color.WHITE);
		Dimension size = new Dimension(305, 370);
		this.setPreferredSize(size);

		FlowLayout fl = new FlowLayout(FlowLayout.LEFT);
		fl.setHgap(5);
		this.setLayout(fl);

		modes = new JComboBox();
		modes.setToolTipText("Select the desired resolution");
		modes.setPreferredSize(new Dimension(150, 20));

		rez = new JComboBox();
		rez.setToolTipText("Set the refresh rate for fullscreen mode");
		rez.setPreferredSize(new Dimension(50, 20));

		click = new JButton();
		click.setText("Play!");
		click.setPreferredSize(new Dimension(90, 20));

		fullscreen = new JCheckBox();
		fullscreen.setPreferredSize(new Dimension(100, 20));
		fullscreen.setBackground(Color.WHITE);
		fullscreen.setText("Fullscreen");
		fullscreen.setToolTipText("Enable fullscreen mode");

		final JFrame pf = parentFrame;

		JPanel spacer = new JPanel();
		spacer.setBackground(java.awt.Color.WHITE);
		Dimension size3 = new Dimension(70, 5);
		spacer.setPreferredSize(size3);

		JPanel spacer2 = new JPanel();
		spacer2.setBackground(java.awt.Color.WHITE);
		Dimension size4 = new Dimension(70, 5);
		spacer2.setPreferredSize(size4);

		subPanel = new JPanel();
		subPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		subPanel.setBackground(gray);
		Dimension size2 = new Dimension(305, 325);
		subPanel.setPreferredSize(size2);
		subPanel.setLayout(fl);

		final JButton options = new JButton();
		options.setText("Show Options");
		options.setPreferredSize(new Dimension(120, 20));
		options.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				if (!optionsVisible) {
					Dimension dim = new Dimension(320, 425 + Math.max(0, pf.getInsets().bottom));
					pf.setPreferredSize(dim);
					pf.setMinimumSize(dim);
					pf.setMaximumSize(dim);
					pf.setSize(dim);
					pf.validate();
					optionsVisible = true;
					options.setText("Hide Options");
				} else {
					Dimension dim = new Dimension(320, 80 + Math.max(0, pf.getInsets().bottom));
					pf.setPreferredSize(dim);
					pf.setMinimumSize(dim);
					pf.setMaximumSize(dim);
					pf.setSize(dim);
					pf.validate();
					optionsVisible = false;
					options.setText("Show Options");
				}
			}
		});

		JLabel rt = new JLabel("Shadow mapping quality:", JLabel.CENTER);
		rt.setPreferredSize(new Dimension(290, 20));
		subPanel.add(rt);

		shadows = new JSlider(JSlider.HORIZONTAL, 0, 4, guessedShadowValue);
		shadows.setPreferredSize(new Dimension(290, 50));
		shadows.setBackground(gray);
		shadows.setSnapToTicks(true);
		shadows.setPaintTrack(true);
		shadows.setPaintTicks(true);
		shadows.setMajorTickSpacing(1);
		shadows.setPaintLabels(true);
		shadows.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
		Dictionary<Integer, JLabel> labels = shadows.getLabelTable();
		labels.put(0, new JLabel("off"));
		labels.put(1, new JLabel("very low"));
		labels.put(2, new JLabel("low"));
		labels.put(3, new JLabel("medium"));
		labels.put(4, new JLabel("high"));
		shadows.setLabelTable(labels);
		shadows.setToolTipText("Higher quality will look better, but may decrease performance");
		subPanel.add(shadows);

		shadowFilter = new JCheckBox();
		shadowFilter.setPreferredSize(new Dimension(200, 20));
		shadowFilter.setBackground(gray);
		shadowFilter.setText("Filter shadow maps");
		shadowFilter.setToolTipText("May smooth shadow edges but can cause artifacts!");
		shadowFilter.setSelected(false);
		subPanel.add(shadowFilter);

		if (!canDoShadows) {
			shadows.setEnabled(false);
			rt.setEnabled(false);
			shadowFilter.setEnabled(false);
		}

		JLabel at = new JLabel("Anti aliasing:", JLabel.CENTER);
		at.setPreferredSize(new Dimension(290, 20));
		subPanel.add(at);

		aa = new JSlider();
		aa.setPreferredSize(new Dimension(290, 50));
		aa.setBackground(gray);
		aa.setMinimum(0);
		aa.setMaximum(4);
		aa.setValue(guessedAAValue);
		aa.setMajorTickSpacing(2);
		aa.setPaintTrack(true);
		aa.setSnapToTicks(true);
		aa.setPaintTicks(true);
		aa.setPaintLabels(true);
		aa.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
		aa.setToolTipText("Higher anti aliasing will look better, but may decrease performance");
		labels = aa.getLabelTable();
		labels.put(0, new JLabel(" none"));
		labels.put(2, new JLabel("2x"));
		labels.put(4, new JLabel("4x"));
		aa.setLabelTable(labels);
		subPanel.add(aa);

		if (!canDoAA) {
			aa.setEnabled(false);
			at.setEnabled(false);
		}

		JLabel ms = new JLabel("Mouse sensivity:", JLabel.CENTER);
		ms.setPreferredSize(new Dimension(290, 20));
		subPanel.add(ms);

		mouse = new JSlider();
		mouse.setPreferredSize(new Dimension(290, 50));
		mouse.setBackground(gray);
		mouse.setMinimum(1);
		mouse.setMaximum(400);
		mouse.setValue(250);
		mouse.setMajorTickSpacing(399);
		mouse.setPaintTrack(true);
		mouse.setPaintLabels(true);
		mouse.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
		labels = mouse.getLabelTable();
		labels.put(1, new JLabel("low"));
		labels.put(400, new JLabel("high"));
		mouse.setLabelTable(labels);
		subPanel.add(mouse);

		JLabel eh = new JLabel("Enhanced graphics options:", JLabel.CENTER);
		eh.setPreferredSize(new Dimension(290, 20));
		subPanel.add(eh);

		highDetail = new JCheckBox();
		highDetail.setBackground(gray);
		highDetail.setText("High detail");
		highDetail.setToolTipText("Use more polygons and particles");
		subPanel.add(highDetail);

		shaders = new JCheckBox();
		shaders.setBackground(gray);
		shaders.setText("Use shaders");
		shaders.setToolTipText("Use shaders for better lighting");
		if (canDoShaders) {
			subPanel.add(shaders);
		}

		highDetail.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean set = highDetail.isSelected();
				if (set) {
					shaders.setSelected(true);
				} else {
					shaders.setSelected(false);
				}
			}
		});

		shaders.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean set = shaders.isSelected();
				if (set) {
					highDetail.setSelected(true);
				}
			}
		});

		skyBox = new JCheckBox();
		skyBox.setBackground(gray);
		skyBox.setText("Sky box");
		skyBox.setToolTipText("Render a sky box");
		subPanel.add(skyBox);

		resolutions = new HashMap<String, List<VideoMode>>();
		VideoMode[] mody = FrameBuffer.getVideoModes(IRenderer.RENDERER_OPENGL);
		if (mody.length > 0) {
			for (int i = 0; i < mody.length; i++) {
				if (mody[i].width >= 512 && mody[i].height >= 480) {
					String key = mody[i].width + "*" + mody[i].height + "*" + mody[i].bpp;
					List<VideoMode> v = resolutions.get(key);
					if (v == null) {
						v = new ArrayList<VideoMode>();
						resolutions.put(key, v);
					}
					v.add(mody[i]);
				}
			}
		} else {
			mody = null;
		}

		List<String> resy = new ArrayList<String>();

		for (String key : resolutions.keySet()) {
			resy.add(key);
		}

		Collections.sort(resy, new VMComparator());

		boolean selected = false;
		for (String key : resy) {
			modes.addItem(key);
			if (!selected) {
				List<VideoMode> vms = resolutions.get(key);
				if (vms.size() > 0) {
					VideoMode vm = vms.get(0);
					if (vm.width >= 800 && vm.height >= 600) {
						selected = true;
						modes.setSelectedItem(key);
					}
				}
			}
		}

		modes.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Object obj = modes.getSelectedItem();
				fillRez(obj);
			}
		});

		fillRez(modes.getSelectedItem());
		this.add(modes);
		this.add(rez);
		this.add(click);
		this.add(fullscreen);
		this.add(spacer);
		this.add(options);
		this.add(spacer2);
		this.add(subPanel);

		load();

		this.setVisible(true);
	}

	private void fillRez(Object obj) {
		List<VideoMode> rezy = resolutions.get(obj);
		rez.removeAllItems();
		List<Integer> rezz = new ArrayList<Integer>();
		if (rezy != null) {
			for (Iterator<VideoMode> itty = rezy.iterator(); itty.hasNext();) {
				rezz.add(Integer.valueOf(((VideoMode) itty.next()).refresh));
			}
		}
		Collections.sort(rezz);

		for (Iterator<Integer> itty = rezz.iterator(); itty.hasNext();) {
			rez.addItem(itty.next());
		}

		rez.setSelectedIndex(0);
	}

	private class VMComparator implements Comparator<String> {
		public int compare(String vm1s, String vm2s) {
			StringTokenizer st = new StringTokenizer(vm1s, "*");
			int w = new Integer(st.nextToken()).intValue();
			int h = new Integer(st.nextToken()).intValue();
			int b = new Integer(st.nextToken()).intValue();
			VideoMode vm1 = new VideoMode(w, h, b, -1, -1);

			st = new StringTokenizer(vm2s, "*");
			w = new Integer(st.nextToken()).intValue();
			h = new Integer(st.nextToken()).intValue();
			b = new Integer(st.nextToken()).intValue();
			VideoMode vm2 = new VideoMode(w, h, b, -1, -1);

			int res = vm2.bpp - vm1.bpp;
			if (res == 0) {
				res = vm1.width - vm2.width;
				if (res == 0) {
					res = vm1.height - vm2.height;
				}
			}
			return res;
		}
	}

	private void load() {
		System.out.println("Loading configuration...");
		InputStream fs = null;
		try {
			fs = new FileInputStream(new File(getConfigFile()));
			probs = new Properties();
			probs.load(fs);

			String hardware = (String) probs.get("hardware");
			if (hardware.equals(hw)) {
				String width = (String) probs.get("width");
				String height = (String) probs.get("height");
				String color = (String) probs.get("color");
				String mode = width + "*" + height + "*" + color;
				modes.setSelectedItem(mode);

				shadowFilter.setSelected(Boolean.valueOf((String) probs.get("filtering")).booleanValue());
				fullscreen.setSelected(Boolean.valueOf((String) probs.get("fullscreen")).booleanValue());

				rez.setSelectedItem(Integer.valueOf((String) probs.get("refresh")));
				Config.glShadowZBias = Float.valueOf((String) probs.get("zbias")).floatValue();
				shadows.setValue(Integer.valueOf((String) probs.get("shadows")).intValue());
				aa.setValue(Integer.valueOf((String) probs.get("AA")).intValue());

				highDetail.setSelected(Boolean.valueOf((String) probs.get("highdetail")).booleanValue());
				shaders.setSelected(Boolean.valueOf((String) probs.get("shaders")).booleanValue());
				skyBox.setSelected(Boolean.valueOf((String) probs.get("skybox")).booleanValue());
			} else {
				JOptionPane.showMessageDialog(null, "Graphics hardware has changed. Setting new defaults!");
			}
			mouse.setValue(Integer.valueOf((String) probs.get("mouse")).intValue());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (fs != null) {
				try {
					fs.close();
				} catch (Exception e) {
				}
			}
		}

		if (TalkBack.mayTalkBack()) {
			int res = JOptionPane
					.showConfirmDialog(
							this,
							"Do you want to allow Robombs to transfer some configuration information and an\nin-game screen shot to a remote server to further improve the game in the future?\nNo personal data will be transfered and it happens only once.\nYou won't be bothered again! Thank you!",
							"Help to improve the game?", JOptionPane.YES_NO_OPTION);
			if (res != 0) {
				TalkBack.noTalkBack();
			}
		}
	}

	public void save() {
		System.out.println("Saving configuration...");
		OutputStream fs = null;
		try {
			fs = new FileOutputStream(new File(getConfigFile()));
			if (probs == null) {
				probs = new Properties();
			}
			probs.put("hardware", hw);
			List<VideoMode> vms = resolutions.get(modes.getSelectedItem());
			if (vms.size() > 0) {
				VideoMode vm = vms.get(0);
				probs.put("width", String.valueOf(vm.width));
				probs.put("height", String.valueOf(vm.height));
				probs.put("color", String.valueOf(vm.bpp));
			}

			probs.put("refresh", String.valueOf(rez.getSelectedItem()));
			probs.put("filtering", String.valueOf(shadowFilter.isSelected()));
			probs.put("fullscreen", String.valueOf(fullscreen.isSelected()));
			probs.put("mouse", String.valueOf(mouse.getValue()));
			probs.put("zbias", String.valueOf(Config.glShadowZBias));
			probs.put("shadows", String.valueOf(shadows.getValue()));
			probs.put("AA", String.valueOf(aa.getValue()));

			probs.put("shaders", String.valueOf(shaders.isSelected()));
			probs.put("highdetail", String.valueOf(highDetail.isSelected()));
			probs.put("skybox", String.valueOf(skyBox.isSelected()));

			probs.store(fs, "Robombs game configuration");

			Globals.shadowMode = shadows.getValue() + "/" + shadowFilter.isSelected();
			Globals.setEnhancedGraphics(highDetail.isSelected());
			Globals.useShaders = shaders.isSelected() && highDetail.isSelected();
			Globals.normalMapping = Globals.useShaders;
			// Globals.normalMapping = false;
			Globals.skyBox = skyBox.isSelected();

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (fs != null) {
				try {
					fs.close();
				} catch (Exception e) {
				}
			}
		}
	}

	private String getConfigFile() {
		String home = System.getProperty("user.home");
		if (home == null) {
			home = ".";
		}
		return home + File.separator + "robombs.cfg";
	}

}
