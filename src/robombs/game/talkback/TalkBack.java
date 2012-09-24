package robombs.game.talkback;

import com.threed.jpct.*;

import java.net.*;
import java.awt.*;
import java.awt.image.*;
import javax.imageio.*;
import javax.imageio.stream.*;
import java.io.*;
import java.util.*;

import robombs.game.*;
import robombs.clientserver.*;

public class TalkBack {

	private static boolean mayTalkBack=true;
	
	public static boolean mayTalkBack() {
		if (!mayTalkBack) {
			return false;
		}
		String home=System.getProperty("user.home");
	    if (home==null) {
		   home=".";
	    }
	    File mtb=new File(home+File.separator+"robombs.tbk");
	    if (mtb.exists()) {
	    	return false;
	    }
	    return true;
	}
	
	public static void noTalkBack() {
		String home=System.getProperty("user.home");
	    if (home==null) {
		   home=".";
	    }
	    File mtb=new File(home+File.separator+"robombs.tbk");
	    try {
	    	mtb.createNewFile();
	    } catch(Exception e) {
	    	e.printStackTrace();
	    }
	    mayTalkBack=false;
	}
	
	public static void talkBack(final FrameBuffer fb) {
		
		if (!mayTalkBack) {
			return;
		}
		
		final Image img=fb.getOutputBuffer();
		
		new Thread() {
			public void run() {
				try {
					noTalkBack();
					URL url=new URL("http://jpct.de/talkback/store.php");
					HttpURLConnection conn = (HttpURLConnection)url.openConnection();
					
					conn.setDoOutput(true);
					conn.setDoInput(true);
					conn.setUseCaches(false);
					conn.setRequestMethod("POST");
					
					OutputStream out=conn.getOutputStream();
					OutputStreamWriter wr = new OutputStreamWriter(out);
		
					Iterator<ImageWriter> itty = ImageIO.getImageWritersBySuffix("jpg");
					if (itty.hasNext()) {
				        ImageWriter iw = (ImageWriter) itty.next();
				        
				        ByteArrayOutputStream bos=new ByteArrayOutputStream();
				        
				        ImageWriteParam iwp = iw.getDefaultWriteParam();
				        iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
				        iwp.setCompressionQuality(0.85f); 
				        
				        ImageOutputStream ios = ImageIO.createImageOutputStream(bos);
				        iw.setOutput(ios);
				        iw.write(null,new IIOImage((RenderedImage) img,null,null),iwp);
				        ios.close();
					
				        sun.misc.BASE64Encoder benc=new sun.misc.BASE64Encoder();
				        
				        String id=System.currentTimeMillis()+"_"+(int)(Math.random()*100000);
				        
				        wr.write("id="+id);
				        wr.write("&width="+fb.getOutputWidth());
				        wr.write("&height="+fb.getOutputHeight());
				        wr.write("&aa="+fb.getSamplingMode());
				        wr.write("&version="+Globals.GAME_VERSION);
				        wr.write("&jpct="+Config.getVersion());
				        wr.write("&java="+URLEncoder.encode(System.getProperty("java.version"), "UTF-8"));
				        wr.write("&vm="+URLEncoder.encode(System.getProperty("java.vm.name"), "UTF-8"));
				        wr.write("&vendor="+URLEncoder.encode(System.getProperty("java.vm.vendor"), "UTF-8"));
				        wr.write("&arch="+URLEncoder.encode(System.getProperty("os.arch"), "UTF-8"));
				        wr.write("&os="+URLEncoder.encode(System.getProperty("os.name"), "UTF-8"));
				        wr.write("&osversion="+URLEncoder.encode(System.getProperty("os.version"), "UTF-8"));
				        wr.write("&adapter="+URLEncoder.encode(Globals.graphicsAdapter, "UTF-8"));
				        wr.write("&shadows="+URLEncoder.encode(Globals.shadowMode, "UTF-8"));
				        wr.write("&cpus="+Runtime.getRuntime().availableProcessors());
				        
				        wr.write("&pic=");
				        ByteArrayOutputStream sbo=new ByteArrayOutputStream();
				        
				        benc.encodeBuffer(new ByteArrayInputStream(bos.toByteArray()), sbo);
				        
				        wr.write(URLEncoder.encode(new String(sbo.toByteArray(), "UTF-8"), "UTF-8"));
				        wr.flush();
				        wr.close();
				        out.flush();
				        out.close();
				        
				         char[] buffy=new char[1000];
				         StringBuffer html=new StringBuffer();
	
				         InputStream in=conn.getInputStream();
				         InputStreamReader reader=new InputStreamReader(in);
	
				         int len=0;
	
				         do {
				            len=reader.read(buffy, 0, buffy.length);
				            if (len!=-1) {
				               html.append(buffy, 0, len);
				            }
				         } while (len!=-1);

				         reader.close();
				         in.close();
				        String res=html.toString();
				        if (res.startsWith("ok/")) {
				         NetLogger.log("Talkback data transfered!");
				        } else {
				        	NetLogger.log("Talkback data not transfered!");
				        	System.err.println(res);
				        }
					}
					
					out.close();
				} catch (Exception e) {
					NetLogger.log("Talkback data not transfered!");
					e.printStackTrace();
				}
			}													
		}.start();
	}
}
