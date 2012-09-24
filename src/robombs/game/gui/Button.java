package robombs.game.gui;

import robombs.game.util.*;

import com.threed.jpct.*;
import com.threed.jpct.util.*;

public class Button extends GUIComponent{
       private String label="";
       private int xp = 0;
       private int yp = 0;
       private int xs = 0;
       private int ys = 0;
       private GUIListener bl=null;
       private boolean clicked=false;
       private boolean hideLabel=false;

       public Button(int xpos, int ypos, int xdim, int ydim) {
          this.xp=xpos;
          this.yp=ypos;
          this.xs=xdim;
          this.ys=ydim;
      }

      public void setHideLabel(boolean hide) {
          hideLabel=hide;
      }

      public void setLabel(String label) {
          this.label=label;
      }

      public void setListener(GUIListener bl) {
          this.bl=bl;
      }

      public boolean evaluateInput(MouseMapper mouse, KeyMapper keyMapper) {
          boolean has=super.evaluateInput(mouse, keyMapper);
          if (!has && isVisible()) {
              int xpos = getParentX();
              int ypos = getParentY();

              boolean input = false;
              int x = mouse.getMouseX() - xpos;
              int y = mouse.getMouseY() - ypos;
              if (mouse.buttonDown(0)) {
                  if (x >= xp && x <= xp + xs && y >= yp && y <= yp + ys) {
                      if (!clicked) {
                          if (bl != null) {
                              bl.elementChanged(label, null);
                          }
                      }
                      clicked = true;
                  } 
              } else {
                  clicked = false;
              }
              return input;
          } else {
              return has;
          }
      }

      public void draw(FrameBuffer buffer) {
          if (visible) {
              if (!hideLabel) {
                  int xc = xs / 2 - (TextBlitter.getWidth(label)/2) + xp;
                  TextBlitter.blitText(buffer, label, getParentX() + xc, getParentY() + yp);
              }
              super.draw(buffer);
          }
      }
   }
