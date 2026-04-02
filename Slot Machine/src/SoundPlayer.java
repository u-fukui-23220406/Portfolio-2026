import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

public class SoundPlayer {
	
    public SoundPlayer() {
    	
    }

    public void playSound(String fileName) {
    	try {
            // src/sounds/xxx.wav をリソースとして読み込む
            // 先頭に / をつけるのがポイントです
            java.net.URL url = getClass().getResource("/sounds/" + fileName);
            
            if (url == null) {
                System.err.println("音源ファイルが見つかりません: " + fileName);
                return;
            }

            AudioInputStream audioStream = AudioSystem.getAudioInputStream(url);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            clip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}