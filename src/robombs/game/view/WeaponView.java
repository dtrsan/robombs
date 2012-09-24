package robombs.game.view;

import robombs.game.util.*;

import com.threed.jpct.*;

/**
 * A view that represents a weapon. A weapon is view-only. There is no server side representation of it.
 */
public class WeaponView extends Object3D {
 
	static final private long serialVersionUID=1L;
	
    private static Object3D bluePrint = null;

    static { 
        try {
            SimpleStream ss = new SimpleStream("data/weapon.md2");
            bluePrint = new Object3D(Loader.loadMD2(ss.getStream(), 0.22f));//
            bluePrint.translate(0, 4.5f, 0);
            bluePrint.translateMesh();
            bluePrint.getTranslationMatrix().setIdentity();
            bluePrint.setTexture("weapon");
            
            // This invader weapon file doesn't follow the standard naming convention. We have to 
            // regroup the animations...that sux!
            
            /*
                {   0,  39,  9 },   // STAND
			    {  40,  45, 10 },   // RUN
			    {  46,  53, 10 },   // ATTACK
			    {  54,  57,  7 },   // PAIN_A
			    {  58,  61,  7 },   // PAIN_B
			    {  62,  65,  7 },   // PAIN_C
			    {  66,  71,  7 },   // JUMP
			    {  72,  83,  7 },   // FLIP
			    {  84,  94,  7 },   // SALUTE
			    {  95, 111, 10 },   // FALLBACK
			    { 112, 122,  7 },   // WAVE
			    { 123, 134,  6 },   // POINT
			    { 135, 153, 10 },   // CROUCH_STAND
			    { 154, 159,  7 },   // CROUCH_WALK
			    { 160, 168, 10 },   // CROUCH_ATTACK
			    { 169, 172,  7 },   // CROUCH_PAIN
			    { 173, 177,  5 },   // CROUCH_DEATH
			    { 178, 183,  7 },   // DEATH_FALLBACK
			    { 184, 189,  7 },   // DEATH_FALLFORWARD
			    { 190, 197,  7 },   // DEATH_FALLBACKSLOW
			    { 198, 198,  5 },   // BOOM
             */

            int[] groups=new int[]{39,45,53,65,71,83,94,111,122,134,153,159,168,172};
            
            Animation anim=bluePrint.getAnimationSequence();
            Mesh[] meshs=anim.getKeyFrames();
            
            Animation re=new Animation(173);
            
            int s=0;
            for (int i=0; i<groups.length; i++){
            	re.createSubSequence("anim_"+i);
            	for (int p=s; p<=groups[i]; p++) {
            		re.addKeyFrame(meshs[p]);
            	}
            	s=groups[i]+1;
            }
            bluePrint.setAnimationSequence(re);
            
            ss.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a new weapon view.
     */
    public WeaponView() {
        super(bluePrint);
    }
}
