package robombs.game.startup;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.*;

import javax.swing.JFrame;

public class ResolutionFrame {

	private static final long serialVersionUID = 1L;

	public ResolutionFrame(final SelectionListener sl) {
		final JFrame jf = new JFrame("Please select a configuration...");
		
		jf.setBackground(java.awt.Color.WHITE);
		jf.setResizable(false);
		jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		final ResolutionPanel rp=new ResolutionPanel(jf);
		ActionListener al=new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				new Thread() {
					public void run() {
						try {
							rp.save();
							sl.selected(rp.getMode(), rp.isFullscreen(), rp.getShadowQuality(), rp.getShadowFiltering(), rp.getAntiAliasingMode(), rp.getMouseSpeed());
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}.start();
				jf.setVisible(false);
				jf.dispose();
			}
		};
		rp.setActionListener(al);
		jf.add(rp);
		Dimension dim=new Dimension(320,80);
		jf.setPreferredSize(dim);
		jf.setMinimumSize(dim);
		jf.setMaximumSize(dim);
		jf.setSize(dim);
		jf.setVisible(true);
		int add=Math.max(0, jf.getInsets().bottom);
		dim=new Dimension(320,80+add);
		jf.setPreferredSize(dim);
		jf.setMinimumSize(dim);
		jf.setMaximumSize(dim);
		jf.setSize(dim);
		jf.pack();
		jf.setLocationRelativeTo(null);
	}
}
