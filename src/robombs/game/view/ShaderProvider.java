package robombs.game.view;

import java.util.HashMap;

import robombs.game.Globals;
import robombs.game.util.SimpleStream;

import com.threed.jpct.Loader;
import com.threed.jpct.Logger;
import com.threed.jpct.Object3D;
import com.threed.jpct.util.GLSLShader;

/**
 * 
 * @author EgonOlsen
 * 
 */
public class ShaderProvider {

	private static HashMap<String, GLSLShader> shaders = new HashMap<String, GLSLShader>();

	static {

		Logger.log("Loading shaders...");
		String vertex = Loader.loadTextFile(new SimpleStream("data/glsl/phong/vertexshader.glsl").getStream());
		String fragment = Loader.loadTextFile(new SimpleStream("data/glsl/phong/fragmentshader.glsl").getStream());

		GLSLShader shader = new GLSLShader(vertex, fragment);
		shader.setDelayedDisabling(Globals.optimizeShaders);
		shaders.put("phong", shader);

		vertex = Loader.loadTextFile(new SimpleStream("data/glsl/normalmapping/vertexshader.glsl").getStream());
		fragment = Loader.loadTextFile(new SimpleStream("data/glsl/normalmapping/fragmentshader.glsl").getStream());

		shader = new GLSLShader(vertex, fragment);
		shader.setDelayedDisabling(Globals.optimizeShaders);
		shader.setStaticUniform("colorMap", 0);
		shader.setStaticUniform("normalMap", 1);
		shader.setStaticUniform("invRadius", 0.0005f);
		shaders.put("normals", shader);
	}

	public static void setShader(String name, Object3D obj) {
		if (Globals.useShaders) {
			GLSLShader shader = shaders.get(name);
			if (shader != null) {
				obj.setRenderHook(shader);
			}
		}
	}
}
