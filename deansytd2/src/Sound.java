import java.applet.*;
import java.net.URL;
public class Sound // Plays one audio file
{
	private AudioClip song; // Sound player
	private URL songPath; // Sound path
	Sound(String filename)
	{
  try
  {
 	 this.songPath = getClass().getClassLoader().getResource(filename);
 	 this.song = Applet.newAudioClip(this.songPath); // Load the Sound
  }
  catch(Exception e){} // Satisfy the catch
	}
	public void playSound()
	{
  this.song.loop(); // Play 
	}
	public void stopSound()
	{
  this.song.stop(); // Stop
	}
	public void playSoundOnce()
	{
  this.song.play(); // Play only once
	}
}

