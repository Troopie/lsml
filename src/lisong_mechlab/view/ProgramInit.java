package lisong_mechlab.view;

import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.WString;
import com.sun.jna.ptr.PointerByReference;
import lisong_mechlab.model.chassi.Chassi;
import lisong_mechlab.model.chassi.ChassiDB;
import lisong_mechlab.model.item.Item;
import lisong_mechlab.model.item.ItemDB;
import lisong_mechlab.model.loadout.export.LsmlProtocolIPC;

import javax.swing.*;
import java.awt.*;
import java.util.Date;


/**
 * This class handles the initial program startup. Things that need to be done before the {@link LSML} instance is
 * created. And it does it while showing a nifty splash screen!
 * 
 * @author Emily Björk
 */
public class ProgramInit extends JFrame{
   private static final long  serialVersionUID   = -2877785947094537320L;
   private static final long  MIN_SPLASH_TIME_MS = 20;
    private static native NativeLong SetCurrentProcessExplicitAppUserModelID(WString appID);
    private static native NativeLong GetCurrentProcessExplicitAppUserModelID(PointerByReference appID);
   private static ProgramInit instance;
   private static LSML        instanceL;
   public static Image        programIcon;

   private String             progressSubText    = "";
   private String             progressText       = "";

   private class BackgroundImage extends JComponent{
      private static final long serialVersionUID = 2294812231919303690L;
      private Image             image;

      public BackgroundImage(Image anImage){
         image = anImage;
      }

      @Override
      protected void paintComponent(Graphics g){
         g.drawImage(image, 0, 0, this);
         int penX = 20;
         int penY = 250;
         g.setColor(Color.WHITE);
         g.drawString(progressText, penX, penY);
         penY += 15;
         g.drawString(progressSubText, penX, penY);
      }
   }

   ProgramInit(){
      instance = this;
      SwingUtilities.invokeLater(new Runnable(){

         @Override
         public void run(){
            Image splash = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/resources/splash.png"));
            setContentPane(new BackgroundImage(splash));
            programIcon = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/resources/icon.png"));
            setIconImage(programIcon);
            setResizable(false);
            setUndecorated(true);
            setTitle("loading...");
            setSize(350, 350);

            // This works for multi-screen configurations in linux as well.
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            DisplayMode mode = ge.getDefaultScreenDevice().getDisplayMode();

            setLocation(mode.getWidth() / 2 - getSize().width / 2, mode.getHeight() / 2 - getSize().height / 2);
            setVisible(true);
            getRootPane().setBorder(BorderFactory.createLineBorder(Color.BLACK));
            getRootPane().putClientProperty("Window.shadow", Boolean.TRUE);
         }
      });
   }

   public static void setProcessText(String aString){
      if( null != instance ){
         instance.progressText = aString;
         instance.repaint();
      }
   }

   public static void setSubText(String aString){
      if( null != instance ){
         instance.progressSubText = aString;
         instance.repaint();
      }
   }

   public boolean waitUntilDone(){
      long startTimeMs = new Date().getTime();

      try{
         @SuppressWarnings("unused")
         // Causes static initialization to be ran.
         Item bap = ItemDB.BAP;

         @SuppressWarnings("unused")
         // Causes static initialization to be ran.
         Chassi chassi = ChassiDB.lookup("JR7-D");
      }
      catch( Throwable e ){
         JOptionPane.showMessageDialog(this,
                                       "Unable to find/parse game data files!\nLSML requires an up-to-date installation of MW:Online to parse data files from.");
         e.printStackTrace();
         return false;
      }

      long endTimeMs = new Date().getTime();
      long sleepTimeMs = Math.max(0, MIN_SPLASH_TIME_MS - (endTimeMs - startTimeMs));
      try{
         Thread.sleep(sleepTimeMs);
      }
      catch( Exception e ){
         // No-Op
      }
      dispose();
      instance = null;
      return true;
   }

   public static void main(final String[] args) throws Exception{

       setCurrentProcessExplicitAppUserModelID(ProgramInit.class.getName());
      // Started with an argument, it's likely a LSML:// protocol string, send it over the IPC and quit.
      if( args.length > 0 ){
         if( LsmlProtocolIPC.sendLoadout(args[0]) )
            return; // Message received we can close this program.
      }

      try{
         // Static global initialization. Stuff that needs to be done before anything else.
         UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
         JFrame.setDefaultLookAndFeelDecorated(true);
      }
      catch( Exception e ){
         JOptionPane.showMessageDialog(null, "Unable to set default look and feel. Something is seriously wrong with your java install!\nError: " + e);
      }

      ProgramInit splash = new ProgramInit();
      if( !splash.waitUntilDone() ){
         System.exit(1);
      }

      javax.swing.SwingUtilities.invokeLater(new Runnable(){
         @Override
         public void run(){
            try{
               instanceL = new LSML();

               if( args.length > 0 )
                  instanceL.mechLabPane.openLoadout(instanceL.loadoutCoder.parse(args[0]));
            }
            catch( Exception e ){
               JOptionPane.showMessageDialog(null, "Unable to start! Error: " + e);
            }
         }
      });
   }



    public static void setCurrentProcessExplicitAppUserModelID(final String appID)
    {
        if (SetCurrentProcessExplicitAppUserModelID(new WString(appID)).longValue() != 0)
            throw new RuntimeException("unable to set current process explicit AppUserModelID to: " + appID);
    }


    static
    {
        Native.register("shell32");
    }

   public static LSML lsml(){
      return instanceL;
   }
}
