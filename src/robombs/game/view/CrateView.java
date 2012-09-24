package robombs.game.view;

import robombs.game.Event;
import robombs.game.Globals;
import robombs.game.sound.SoundManager;
import robombs.game.util.*;
import robombs.game.model.*;

import com.threed.jpct.*;
import com.threed.jpct.util.*;

public class CrateView extends ClientObject {

	static final private long serialVersionUID = 1L;

	private static ClientObject bluePrint = null;
	private static Object3D childBluePrint = null;
	private transient DecalManager decMan = null;
	private boolean exploding = false;
	private float animIndex = 0;
	private ShadowHelper sh = null;
	private boolean itemShown = false;
	private boolean botUsage = false;

	private transient LocalObject lob = null;
	private World world = null;

	static {
		try {
			SimpleStream ss = new SimpleStream("data/crate.3ds");
			bluePrint = new ClientObject(Loader.load3DS(ss.getStream(), 0.09f)[0]);
			ss.close();
			bluePrint.translate(0, -4.3f, -5);
			bluePrint.translateMesh();
			bluePrint.getTranslationMatrix().setIdentity();
			TextureInfo ti = null;
			if (!Globals.normalMapping) {
				bluePrint.setTexture("crate");
			} else {
				ti = new TextureInfo(TextureManager.getInstance().getTextureID("crate"));
				ti.add(TextureManager.getInstance().getTextureID("cratenormals"), TextureInfo.MODE_MODULATE);
				bluePrint.setTexture(ti);
			}

			bluePrint.getMesh().compress();
			bluePrint.build();

			Object3D child = null;
			Animation anim = new Animation(6);
			anim.setClampingMode(Animation.USE_CLAMPING);
			anim.createSubSequence("explosion");

			Loader.setVertexOptimization(false);

			for (int i = 1; i < 6; i++) {
				String name = "data/crate" + i + ".3ds";
				ss = new SimpleStream(name);
				Object3D obj = Loader.load3DS(ss.getStream(), 4.4050189f)[0];
				ss.close();
				obj.translate(0, -5f, -0.6f);
				obj.translateMesh();
				obj.getTranslationMatrix().setIdentity();
				if (!Globals.normalMapping) {
					obj.setTexture("crate");
				} else {
					obj.setTexture(ti);
				}
				obj.build();

				if (i == 1) {
					child = obj;
					child.setMesh(child.getMesh().cloneMesh(true));
				}
				obj.getMesh().compress();
				anim.addKeyFrame(obj.getMesh());
			}
			child.setAnimationSequence(anim);

			Loader.setVertexOptimization(true);

			childBluePrint = child;

			/*
			 * for (int i=0; i<child.getMesh().getBoundingBox().length; i++) {
			 * System.out.print(child.getMesh().getBoundingBox()[i]);
			 * System.out.println("="+bluePrint.getMesh().getBoundingBox()[i]);
			 * }
			 */
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Creates a new crate.
	 */
	public CrateView() {
		super(bluePrint, false);
		enableLazyTransformations();
		if (Globals.normalMapping) {
			ShaderProvider.setShader("normals", this);
		}
	}

	public CrateView(ClientObject obj) {
		super(obj, true);
		enableLazyTransformations();
		if (Globals.normalMapping) {
			ShaderProvider.setShader("normals", this);
		}
	}

	public void botUsage() {
		botUsage = true;
	}

	public void hitByExplosion(CollisionParticipant source, LocalObject obj, DecalManager decal, CollisionEvent ce) {
		if (!isInactive()) {
			Event event = new Event(Event.CRATE_HIT, -99, obj.getObjectID(), -99);
			int[] ids = ce.getPolygonIDs();
			if (ids != null) {
				// The Crates on the bot client don't return those IDs...and
				// they don't have to.
				PolygonManager pm = getPolygonManager();
				SimpleVector normal = pm.getTransformedNormal(ids[0]);
				event.setOrigin(normal);
			}
			source.getEventQueue().add(event);
		}
	}

	public void setDecalManager(DecalManager dm) {
		this.decMan = dm;
	}

	public void setWorld(World world) {
		this.world = world;
	}

	public void init(int id) {
		// This instance of LocalObject is used in the view only. The crates
		// aren't real
		// LocalObject/ClientObject-entities as they are not transfered to the
		// server.
		// However, events and collision listeners need some information from
		// the LocalObject
		lob = new LocalObject(-99);
		lob.setObjectID(id);
	}

	public LocalObject getLocalObject() {
		return lob;
	}

	public void setShadowHelper(ShadowHelper sh) {
		this.sh = sh;
	}

	public boolean isInactive() {
		return !getSuperVisibility();
	}

	public boolean isExploding() {
		return exploding;
	}

	public void setVisibility(boolean vis) {
		if (animIndex < 1) {
			if (!exploding) {
				setSuperVisibility(vis);
				if (child != null) {
					child.setRealVisibility(false);
				}
			} else {
				setSuperVisibility(false);
				if (child != null) {
					child.setRealVisibility(vis);
				}
			}
		} else {
			setSuperVisibility(false);
			if (child != null) {
				child.setRealVisibility(false);
			}
		}
	}

	public void removeUponServerRequest() {
		setVisibility(false);
		setCollisionMode(Object3D.COLLISION_CHECK_NONE);
		if (decMan != null) {
			decMan.parentRemoved(this);
			decMan = null;
		}
	}

	public void process(long ticks, Level level) {
		if (exploding) {
			checkChild();
			float mul = 0.05f;
			if (!botUsage) {
				child.animate(animIndex);
				child.getTranslationMatrix().setIdentity();
				child.translate(0, 8f * animIndex, 0);
				SimpleVector sz = child.getZAxis();
				sz.scalarMul(4f * animIndex);
				child.translate(sz);
			}
			animIndex += ((float) ticks) * mul;
			if (animIndex >= 1 && exploding != false) {
				exploding = false;
				if (!botUsage) {
					child.setRealVisibility(false);
				}
			}
			if (animIndex >= 0.5f && !itemShown) {
				// The item inherits the id from the crate, which is unique and
				// the same on all clients.
				if (level.createItem(getTransformedCenter(), getLocalObject(), sh) && !botUsage) {
					SoundManager.getInstance().play("bubble", getTranslation());
				}
				itemShown = true;
			}
		}
	}

	public boolean isGone() {
		return !getVisibility() && !exploding;
	}

	public void explode(SimpleVector origin) {
		if (!exploding && animIndex < 1) {
			checkChild();
			if (!botUsage) {
				if (origin.x > 0.5f || origin.x < -0.5f) {
					if (origin.x > 0.5f) {
						child.rotateY((float) Math.PI / 2f);
					} else {
						child.rotateY(-(float) Math.PI / 2f);
					}
				} else {
					if (origin.z < 0.5f) {
						child.rotateY((float) Math.PI);
					}
				}
			}

			setSuperVisibility(false);
			if (!botUsage) {
				child.setRealVisibility(true);
			}
			exploding = true;
			setCollisionMode(Object3D.COLLISION_CHECK_NONE);
			if (decMan != null) {
				decMan.parentRemoved(this);
				decMan = null;
			}
		}
	}

	public void addToWorld(World world) {
		super.addToWorld(world);
		this.world = world;
	}

	private void checkChild() {
		if (world == null) {
			throw new RuntimeException("The CrateView instance has to know its world!");
		}
		if (animIndex == 0 && child == null && !botUsage) {
			// Create the child object with the animation inside.
			addChild(childBluePrint);
			if (Globals.compiledObjects) {
				child.compile(true);
			}

			String name = "phong";
			if (Globals.normalMapping) {
				name = "normals";
			}

			ShaderProvider.setShader(name, child);
			world.addObject(child);
		}
	}

}
