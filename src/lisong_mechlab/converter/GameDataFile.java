package lisong_mechlab.converter;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;
import static java.nio.file.FileVisitResult.TERMINATE;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.swing.JOptionPane;

import lisong_mechlab.view.LsmlPreferences;
import lisong_mechlab.view.ProgramInit;

public class GameDataFile{
   private static final String   PREF_GAMEDIR   = "gamedir";
   public static final File      ITEM_STATS_XML = new File("Game/Libs/Items/ItemStats.xml");
   public static final File      MDF_ROOT       = new File("Game/Objects/mechs/");

   private final Map<File, File> entryCache     = new HashMap<File, File>();
   private final Path            gamePath;

   private final ProgramInit     init;

   public GameDataFile() throws IOException{

      init = ProgramInit.getInstance();
      if( null != init )
         init.setProcessText("Searching for game files:");

      String gameDir = LsmlPreferences.getString(PREF_GAMEDIR);
      if( isValidGameDirectory(new File(gameDir).toPath()) ){
         gamePath = new File(gameDir).toPath();
      }
      else{
         Path p = findGameDirectory();
         if( null != p ){
            gamePath = p;
         }
         else
            throw new FileNotFoundException("Couldn't find the game directory!");
      }
      LsmlPreferences.setString(PREF_GAMEDIR, gamePath.toString());

      if( null != init )
         init.setProcessText("Parsing game files...");
   }

   public GameDataFile(File aGameRoot) throws FileNotFoundException{
      init = ProgramInit.getInstance();
      if( isValidGameDirectory(aGameRoot.toPath()) ){
         gamePath = aGameRoot.toPath();
      }
      else{
         throw new FileNotFoundException("The given path doesn't contain the MWO client!");
      }
   }

   /**
    * Will open an input stream to the given game data file.
    * 
    * @param aPath
    *           The path to the file to open, with archive file names expanded. For example
    *           "Game/Objects/mechs/spider/sdr-5k.mdf"
    * @return An {@link InputStream} to the requested file.
    * @throws IOException
    * @throws ZipException
    */
   public InputStream openGameFile(File aPath) throws ZipException, IOException{
      // Try finding a raw file
      {
         File file = new File(gamePath.toFile(), aPath.toString());
         if( file.exists() ){
            return new FileInputStream(file);
         }
      }

      // Try looking in archive cache
      File sourceArchive = null;
      synchronized( entryCache ){
         sourceArchive = entryCache.get(aPath);

         if( null == sourceArchive ){
            // Cache miss! Update cache
            search(aPath, new File(gamePath.toFile(), "Game"));

            // Try again
            sourceArchive = entryCache.get(aPath);
            if( sourceArchive == null ){
               throw new RuntimeException("Failed to find sought for file (" + aPath + ") in the game files, this is most likely a bug!");
            }
         }
      }

      ZipFile zipFile = null;
      byte[] buffer = null;
      try{
         zipFile = new ZipFile(sourceArchive);

         String archivePath = gamePath.relativize(sourceArchive.getParentFile().toPath()).relativize(aPath.toPath()).toString();
         // Fix windows...
         archivePath = archivePath.replaceAll("\\\\", "/");

         ZipEntry entry = zipFile.getEntry(archivePath);
         if( null == entry ){
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while( entries.hasMoreElements() ){
               System.err.println(entries.nextElement());
            }
            throw new RuntimeException("Failed to find sought for file (" + aPath + ") in the game files, this is most likely a bug!");
         }
         int size = (int)entry.getSize();
         buffer = new byte[size];
         InputStream is = zipFile.getInputStream(entry);
         int bytesRead = 0;
         while( bytesRead < size ){
            int res = is.read(buffer, bytesRead, size - bytesRead);
            if( -1 == res ){
               throw new IOException("Couldn't read entire file!");
            }
            bytesRead += res;
         }
      }
      finally{
         if( null != zipFile )
            zipFile.close();
      }
      return new ByteArrayInputStream(buffer);
   }

   private void search(File aLocalPath, File aSearchRoot) throws IOException{
      synchronized( entryCache ){

         Collection<File> visitedArchives = entryCache.values();
         Path relativePath = gamePath.relativize(aSearchRoot.toPath());

         for(File file : aSearchRoot.listFiles()){
            if( file.isDirectory() ){
               search(aLocalPath, file);
            }
            else{
               if( visitedArchives.contains(file) ){
                  continue;
               }
               if( file.getName().toLowerCase().endsWith(".pak") && !file.getName().toLowerCase().contains("french") ){
                  ZipFile zipFile = null;
                  try{
                     zipFile = new ZipFile(file);
                     Enumeration<? extends ZipEntry> entries = zipFile.entries();
                     while( entries.hasMoreElements() ){
                        File key = new File(relativePath.toFile(), entries.nextElement().toString());
                        entryCache.put(key, file);
                     }
                  }
                  catch( IOException exception ){
                     System.err.println(exception);
                  }
                  finally{
                     if( null != zipFile )
                        zipFile.close();
                  }
                  if( entryCache.containsKey(aLocalPath) ){
                     break; // We have put the sought for key into the cache.
                  }
               }
            }
         }
      }
   }

   private boolean isValidGameDirectory(Path aPath){
      File file = new File(aPath.toFile(), "Game/Objects.pak");
      return file.exists();
   }

   private Path findGameDirectory() throws IOException{
      class GameFinder extends SimpleFileVisitor<Path>{
         public Path gameRoot = null;

         @Override
         public FileVisitResult visitFile(Path file, BasicFileAttributes attrs){
            if( null != init )
               init.setSubText(file.toString());
            if( file.endsWith("Game/Objects.pak") ){
               int answer = JOptionPane.showConfirmDialog(null, "Found the game files at: " + file.getParent().getParent().toString()
                                                                + "\nIs this your primary game install?", "Confirm game directory",
                                                          JOptionPane.YES_NO_OPTION);
               if( JOptionPane.YES_OPTION == answer ){
                  gameRoot = file.getParent().getParent();
                  return TERMINATE;
               }
            }
            return CONTINUE;
         }

         @Override
         public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs){
            if( dir.getFileName() != null
                && (dir.getFileName().toString().toLowerCase().equals("windows") || dir.getFileName().toString().toLowerCase().equals("users")) )
               // Skip windows folder, it's big and slow and we don't expect to find the game there.
               return SKIP_SUBTREE;
            return CONTINUE;
         }
      }

      // Look for a quick exit in the default install directories.
      Path defaultPath = getDefaultGameFileLocation();
      if( defaultPath.toFile().exists() ){
         return defaultPath;
      }

      // Walk all the file roots, or drives in windows
      GameFinder finder = new GameFinder();
      File[] roots = File.listRoots();
      for(File root : roots){
         // But only if they have enough space to hold the game install and enough space to be a usable disk (5 Mb)
         if( root.getTotalSpace() > 1024 * 1024 * 1500 && root.getFreeSpace() > 1024 * 1024 * 5 ){
            Files.walkFileTree(root.toPath(), finder);
            if( null != finder.gameRoot ){
               return finder.gameRoot;
            }
         }
      }
      return null;
   }

   private Path getDefaultGameFileLocation(){
      // Uses two variations one for x64 and one for x86
      Path defaultGameFileLocation = FileSystems.getDefault().getPath("C:\\Program Files (x86)\\Piranha Games\\MechWarrior Online");
      if( !defaultGameFileLocation.toFile().exists() ){
         defaultGameFileLocation = FileSystems.getDefault().getPath("C:\\Program Files\\Piranha Games\\MechWarrior Online");
      }
      if( !defaultGameFileLocation.toFile().exists() ){
         defaultGameFileLocation = FileSystems.getDefault().getPath("C:\\Games\\Piranha Games\\MechWarrior Online");
      }
      return defaultGameFileLocation;
   }
}
