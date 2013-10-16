package lisong_mechlab.view;



import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.InputStream;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import lisong_mechlab.model.chassi.Part;
import lisong_mechlab.model.loadout.DynamicSlotDistributor;
import lisong_mechlab.model.loadout.Loadout;
import lisong_mechlab.model.loadout.MechGarage;
import lisong_mechlab.util.MessageXBar;
import lisong_mechlab.util.MessageXBar.Message;
import lisong_mechlab.view.action.CloneLoadoutAction;
import lisong_mechlab.view.action.DeleteLoadoutAction;
import lisong_mechlab.view.action.MaxArmorAction;
import lisong_mechlab.view.action.RenameLoadoutAction;
import lisong_mechlab.view.action.ShareLoadoutAction;
import lisong_mechlab.view.graphs.DamageGraph;

public class LoadoutFrame extends JInternalFrame implements MessageXBar.Reader{
   private static final long serialVersionUID = -9181002222136052106L;
   private static int        openFrameCount   = 0;
   private static final int  xOffset          = 30, yOffset = 30;
   private final Loadout     loadout;
   private final MessageXBar xbar;
   private JMenuItem         addToGarage;
   private LoadoutInfoPanelController loadoutInfoPanelController;

   public LoadoutFrame(Loadout aLoadout, MessageXBar anXBar){
      super(aLoadout.toString(), true, // resizable
            true, // closable
            false, // maximizable
            true);// iconifiable

      xbar = anXBar;
      xbar.attach(this);

      // ...Create the GUI and put it in the zwindow...
      // ...Then set the window size or call pack...

      loadout = aLoadout;

      JMenuBar menuBar = new JMenuBar();
      menuBar.add(createMenuLoadout());
      menuBar.add(createMenuArmor());
      menuBar.add(createMenuGraphs());
      menuBar.add(createMenuShare());
      setJMenuBar(menuBar);
      setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
      // Set the window's location.
      setLocation(xOffset * openFrameCount, yOffset * openFrameCount);
      openFrameCount++;

      final JFXPanel r = new JFXPanel();
      Platform.runLater(new Runnable() {
         @Override
         public void run() {
         initFX("loadoutInfoPanel.fxml", r);
         loadoutInfoPanelController.setUp(loadout, xbar);
         }

         
    });
      
      
      JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, createMechView(aLoadout, anXBar), r);

      sp.setDividerLocation(-1);
      sp.setDividerSize(0);

      setFrameIcon(null);
      setContentPane(sp);

      pack();
      setVisible(true);

      addInternalFrameListener(new InternalFrameAdapter(){
         @Override
         public void internalFrameClosing(InternalFrameEvent e){
            if( !isSaved() ){
               int ans = JOptionPane.showConfirmDialog(LoadoutFrame.this, "Would you like to save " + loadout.getName() + " to your garage?",
                                                       "Save to garage?", JOptionPane.YES_NO_CANCEL_OPTION);
               if( ans == JOptionPane.YES_OPTION ){
                  ProgramInit.lsml().getGarage().add(loadout);
                  dispose();
               }
               if(ans == JOptionPane.NO_OPTION){
                 
                     dispose();
                  
               }
            }
            
         }
      });
   }

   public boolean isSaved(){
      return ProgramInit.lsml().getGarage().getMechs().contains(loadout);
   }

   public Loadout getLoadout(){
      return loadout;
   }
   
   private void initFX(String fxml,JFXPanel JFXPanel){
      
      try{
         loadoutInfoPanelController = (lisong_mechlab.view.LoadoutInfoPanelController)replaceSceneContent(fxml, JFXPanel);
         
      }
      catch( Exception e ){
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      
      
   }
   
   Initializable replaceSceneContent(String fxml, JFXPanel r) throws Exception {
      FXMLLoader loader = new FXMLLoader();
      InputStream in = LSML.class.getResourceAsStream(fxml);
      loader.setBuilderFactory(new JavaFXBuilderFactory());
      loader.setLocation(LSML.class.getResource(fxml));
      AnchorPane page;
      try {
          page = (AnchorPane) loader.load(in);
      } finally {
          in.close();
      } 
      Scene scene = new Scene(page, 300, 700);
      r.setScene(scene);
//      ((LoadoutInfoPanelController)loader.getController()).setUp(loadout, xbar);
      return (Initializable) loader.getController();
  }

   private JPanel createMechView(Loadout aConfiguration, MessageXBar anXBar){
      final JPanel panel = new JPanel();
      panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));

      Dimension padding = new Dimension(5, 0);

      panel.add(Box.createRigidArea(padding));

      DynamicSlotDistributor slotDistributor = new DynamicSlotDistributor(loadout);

      // Right Arm
      {
         final JPanel subPanel = new JPanel();
         subPanel.setLayout(new BoxLayout(subPanel, BoxLayout.PAGE_AXIS));
         subPanel.add(Box.createVerticalStrut(50));
         subPanel.add(new PartPanel(aConfiguration.getPart(Part.RightArm), anXBar, true, slotDistributor));
         subPanel.add(Box.createVerticalGlue());
         panel.add(subPanel);
      }

      panel.add(Box.createRigidArea(padding));

      // Right Torso + Leg
      {
         final JPanel subPanel = new JPanel();
         subPanel.setLayout(new BoxLayout(subPanel, BoxLayout.PAGE_AXIS));
         subPanel.add(new PartPanel(aConfiguration.getPart(Part.RightTorso), anXBar, true, slotDistributor));
         subPanel.add(new PartPanel(aConfiguration.getPart(Part.RightLeg), anXBar, false, slotDistributor));
         subPanel.add(Box.createVerticalGlue());
         panel.add(subPanel);
      }

      panel.add(Box.createRigidArea(padding));

      // Center Torso + Head
      {
         final JPanel subPanel = new JPanel();
         subPanel.setLayout(new BoxLayout(subPanel, BoxLayout.PAGE_AXIS));
         subPanel.add(new PartPanel(aConfiguration.getPart(Part.Head), anXBar, true, slotDistributor));
         subPanel.add(new PartPanel(aConfiguration.getPart(Part.CenterTorso), anXBar, true, slotDistributor));
         subPanel.add(Box.createVerticalGlue());
         panel.add(subPanel);
      }

      panel.add(Box.createRigidArea(padding));

      // Left Torso + Leg
      {
         final JPanel subPanel = new JPanel();
         subPanel.setLayout(new BoxLayout(subPanel, BoxLayout.PAGE_AXIS));
         subPanel.add(new PartPanel(aConfiguration.getPart(Part.LeftTorso), anXBar, true, slotDistributor));
         subPanel.add(new PartPanel(aConfiguration.getPart(Part.LeftLeg), anXBar, false, slotDistributor));
         subPanel.add(Box.createVerticalGlue());
         panel.add(subPanel);
      }

      panel.add(Box.createRigidArea(padding));

      // Left Arm
      {
         final JPanel subPanel = new JPanel();
         subPanel.setLayout(new BoxLayout(subPanel, BoxLayout.PAGE_AXIS));
         subPanel.add(Box.createVerticalStrut(50));
         subPanel.add(new PartPanel(aConfiguration.getPart(Part.LeftArm), anXBar, true, slotDistributor));
         subPanel.add(Box.createVerticalGlue());
         panel.add(subPanel);
      }
      // panel.setVisible(true);
      // panel.validate();
      // setMinimumSize(panel.getSize());
      // setMaximumSize(getMinimumSize());
      // setPreferredSize(getMinimumSize());
      return panel;
   }

   private JMenuItem createMenuItem(String text, ActionListener anActionListener){
      JMenuItem item = new JMenuItem(text);
      item.addActionListener(anActionListener);
      return item;
   }

   private JMenu createMenuShare(){
      JMenu menu = new JMenu("Share!");
      menu.add(new JMenuItem(new ShareLoadoutAction(loadout)));
      return menu;
   }

   private JMenu createMenuLoadout(){
      JMenu menu = new JMenu("Loadout");

      addToGarage = new JMenuItem("Add to garage");
      if( isSaved() )
         addToGarage.setEnabled(false);
      else
         addToGarage.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent aArg0){
               try{
                  // TODO: This should be an Action class
                  ProgramInit.lsml().getGarage().add(loadout);
               }
               catch( IllegalArgumentException e ){
                  JOptionPane.showMessageDialog(LoadoutFrame.this, "Couldn't add to garage! Error: " + e.getMessage());
               }
            }
         });

      menu.add(addToGarage);
      menu.add(new JMenuItem(new RenameLoadoutAction(loadout, KeyStroke.getKeyStroke("R"))));
      menu.add(new JMenuItem(new DeleteLoadoutAction(ProgramInit.lsml().getGarage(), loadout, KeyStroke.getKeyStroke("D"))));

      menu.add(createMenuItem("Load stock", new ActionListener(){
         @Override
         public void actionPerformed(ActionEvent aArg0){
            try{
               loadout.loadStock();
            }
            catch( Exception e ){
               JOptionPane.showMessageDialog(LoadoutFrame.this, "Couldn't load stock loadout! Error: " + e.getMessage());
            }
         }
      }));

      menu.add(createMenuItem("Strip mech", new ActionListener(){
         @Override
         public void actionPerformed(ActionEvent aArg0){
            loadout.strip();
         }
      }));
      
      menu.add(new JMenuItem(new CloneLoadoutAction("Clone", loadout, KeyStroke.getKeyStroke("C"))));
      return menu;
   }

   private JMenu createMenuArmor(){
      JMenu menu = new JMenu("Armor");

      menu.add(createMenuItem("Strip Armor", new ActionListener(){
         @Override
         public void actionPerformed(ActionEvent aArg0){
            loadout.stripArmor();
         }
      }));

      {
         JMenu subMenu = new JMenu("Max Armor");
         menu.add(subMenu);
         subMenu.add(new JMenuItem(new MaxArmorAction("3:1", loadout, 3)));
         subMenu.add(new JMenuItem(new MaxArmorAction("5:1", loadout, 5)));
         subMenu.add(new JMenuItem(new MaxArmorAction("10:1", loadout, 10)));
         subMenu.add(new JMenuItem(new MaxArmorAction("Custom...", loadout, -1)));
      }
      return menu;
   }

   private JMenu createMenuGraphs(){
      JMenu menu = new JMenu("Graphs");

      menu.add(createMenuItem("Damage", new ActionListener(){
         @Override
         public void actionPerformed(ActionEvent aArg0){
            new DamageGraph(loadout, xbar);
         }
      }));
      return menu;
   }

   @Override
   public void receive(Message aMsg){
      if( !aMsg.isForMe(loadout) )
         return;

      if( aMsg instanceof MechGarage.Message ){
         MechGarage.Message msg = (MechGarage.Message)aMsg;
         if( msg.type == MechGarage.Message.Type.LoadoutRemoved ){
            dispose(); // Closes frame
         }
         else if( msg.type == MechGarage.Message.Type.LoadoutAdded ){
            SwingUtilities.invokeLater(new Runnable(){
               @Override
               public void run(){
                  addToGarage.setEnabled(false);
               }
            });
         }
      }
      else if( aMsg instanceof Loadout.Message ){
         Loadout.Message msg = (Loadout.Message)aMsg;
         if( msg.type == Loadout.Message.Type.RENAME ){
            setTitle(loadout.toString());
         }
      }
   }
}
