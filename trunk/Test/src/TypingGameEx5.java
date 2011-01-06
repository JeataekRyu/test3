import java.util.*; 
import java.awt.*; 
import java.awt.event.*; 

class TypingGameEx5 extends Frame 
{ 
      final int FRAME_WIDTH = 400; 
      final int FRAME_HEIGHT = 300; 
      
      final int SCREEN_WIDTH; 
      final int SCREEN_HEIGHT; 

      int speed = 500; // 단어가 떨어지는 속도... 높을 수록 느리다. 
      int interval = 2 * 1000; // 새로운 단어가 나오는 간격 

      int score = 0; 
      int life = 3; 
      int curLevel = 0; 
      final int MAX_LEVEL; 

      boolean isPlaying = false; 

      WordGenerator wg = null; // 단어를 생성하는 쓰레드 
      WordDropper wm = null; // 단어를 떨어뜨리는 쓰레드 

      FontMetrics fm; // 화면에서의 글자 길이를 구하는데 사용 
      ThreadGroup virusGrp = new ThreadGroup("virus"); // 바이러스 쓰레드들의 그룹 

      String[][] data = { 
            {"태연","유리","윤아","효연","수영","서현","티파니","써니","제시카"}, 
            {"태연","유리","윤아","효연","수영","서현","티파니","써니","제시카"}, 
            {"태연","유리","윤아","효연","수영","서현","티파니","써니","제시카"}, 
            {"태연","유리","윤아","효연","수영","서현","티파니","써니","제시카"}, 
      }; 

      final Level[] LEVEL = { 
            new Level(500, 2000, 1000, data[0]), 
            new Level(250, 1500, 2000, data[1]), 
            new Level(120, 1000, 3000, data[2]), 
            new Level(100, 500, 4000, data[3]), 
      }; 

      Vector words = new Vector(); 

      TextField tf = new TextField(); 
      Panel pScore = new Panel(new GridLayout(1,3)); 
      Label lbLevel = new Label("Level:"+curLevel, Label.CENTER); 
      Label lbScore = new Label("Score:"+score, Label.CENTER); 
      Label lbLife = new Label("Life:"+life, Label.CENTER); 
      MyCanvas screen = new MyCanvas(); 

      TypingGameEx5() { 
            this("Typing game ver1.0"); 
      } 

      TypingGameEx5(String title) { 
            super(title); 

            pScore.setBackground(Color.YELLOW); 
            pScore.add(lbLevel); 
            pScore.add(lbScore); 
            pScore.add(lbLife); 
            add(pScore, "North"); 
            add(screen, "Center"); 
            add(tf, "South"); 

            MyEventHandler handler = new MyEventHandler(); 
            addWindowListener(handler); 
            tf.addActionListener(handler); 

            setBounds(500, 200, FRAME_WIDTH, FRAME_HEIGHT); 
            setResizable(false); 
            setVisible(true); 

            SCREEN_WIDTH = screen.getWidth(); 
            SCREEN_HEIGHT = screen.getHeight(); 
            MAX_LEVEL = LEVEL.length-1; 

            fm = getFontMetrics(getFont()); 
      } 

      public void repaint() { 
            super.repaint(); 
            screen.repaint(); 
      } 

      public void delay(int millis) { 
            try { 
                  Thread.sleep(millis); 
            } catch(Exception e) {} 
      } 

      public void start() { 
            showLevel(0); 
            isPlaying = true; 

            wg = new WordGenerator(); 
            wg.start(); 

            wm = new WordDropper(); 
            wm.start(); 
      } 

      public Level getLevel(int level) { 
            if(level > MAX_LEVEL) level = MAX_LEVEL; 
            if(level < 0) level = 0; 

            return LEVEL[level]; 
      } 

      public boolean levelUpCheck() { 
            Level lvl = getLevel(curLevel); 

            return score >= lvl.levelUpScore; 
      } 

      public synchronized int getCurLevel() { 
            return curLevel; 
      } 

      public synchronized void levelUp() { 
            virusGrp.interrupt(); 


            Level lvl = getLevel(++curLevel);             

            lbLevel.setText("Level:"+curLevel); 
            words.clear(); 
            screen.clear(); 
            showLevel(curLevel); 
      
            speed = lvl.speed; 
            interval = lvl.interval; 
      } 

      public void showLevel(int level) { 
            String tmp = "Level "+level; 
            showTitle(tmp, 1 * 1000); 
      } 

      public void showTitle(String title, int time) { 
            Graphics g = screen.getGraphics(); 

Font titleFont = new Font("Serif",Font.BOLD, 20); 
            g.setFont(titleFont); 

            FontMetrics fm = getFontMetrics(titleFont); 
            int width = fm.stringWidth(title); 

            g.drawString(title, (SCREEN_WIDTH-width)/2, SCREEN_HEIGHT/2); 
            delay(time); 
      } 

      public static void main(String[] args) { 
            TypingGameEx5 win = new TypingGameEx5(); 
            win.start(); 
      } 

      class WordDropper extends Thread { 
            public void run() { 
                  outer: 
                  while(isPlaying) { 
                        delay(speed); 
                        for(int i=0; i < words.size();i++) { 
                              Word tmp = (Word)words.get(i); 

                              tmp.y += tmp.step; 

                              if(tmp.y >= SCREEN_HEIGHT) { 
                                    tmp.y = SCREEN_HEIGHT; 
                                    words.remove(tmp); 
                                    life--; 
                                    lbLife.setText("Life:"+life); 
                                    break; 
                              } 

                              if(life <= 0) { 
                                    isPlaying = false; 
                                    showTitle("Game Over", 0); 

                                    break outer; 
                              } 
                        } // for 

                        repaint(); 
                  } 
            } // end of run() 
      } 

      class WordGenerator extends Thread { 
            public void run() { 
                  while(isPlaying) { 
                        String[] data = LEVEL[getCurLevel()].data; 

                        int rand = (int)(Math.random()*data.length); 

                        // 약 10번에 한번 꼴로 바이러스를 생성한다. 
                        boolean isVirus = ((int)(Math.random()*10) + 1) / 10 != 0; 

                        Word word = new Word(data[rand], isVirus); 
                        words.add(word); 
                        delay(interval); 
                  } 
            } // end of run() 
      } 

      class MyCanvas extends Canvas { 
            public void clear() { 
                  Graphics g = getGraphics(); 
                  g.clearRect(0, 0, FRAME_WIDTH, FRAME_HEIGHT); 
            } 

            public void paint(Graphics g) { 
                  clear(); 

                  for(int i=0; i < words.size(); i++) { 
                        Word tmp = (Word)words.get(i); 
                        g.setColor(tmp.color); 
                        g.drawString(tmp.word, tmp.x,tmp.y);                   
                  } 
            } 
      } 

      class VirusThread extends Thread { 
            public VirusThread(ThreadGroup group, String name) { 
                  super(group, name); 
            } 

            public void run() { 
                  int rand = (int)(Math.random()*5); 

                  int oldValue = 0; 
                  int virusTime = 10 * 1000; // 바이러스 동작시간을 10초로 설정한다. 

                  switch(rand) { 
                        case 0: 
                              speed = speed / 2; 
                              break; 
                        case 1: 
                              interval = interval / 2; 
                              break; 
                        case 2: 
                              speed = speed * 2; 
                              break; 
                        case 3: 
                              interval = interval * 2; 
                              break; 
                        case 4: 
                              words.clear(); 
                              break; 
                  } // swtich 

                  delay(virusTime); 

                  int curLevel = getCurLevel(); 
                  speed = LEVEL[curLevel].speed; 
                  interval = LEVEL[curLevel].interval; 
            } // end of run() 
      } 

      class Level { 
            int speed; 
            int interval; 
            int levelUpScore; 
            String[] data; 

            Level(int speed, int interval, int levelUpScore, String[] data) { 
                  this.speed = speed; 
                  this.interval = interval; 
                  this.levelUpScore = levelUpScore; 
                  this.data = data; 
            } 
      } // GameLevel 

      class Word { 
            String word = ""; 
            int x = 0; 
            int y = 0; 
            int step = 5; 

            Color color = Color.BLACK; 
            boolean isVirus = false; 
            
            Word(String word) { 
                  this(word, 10, false); 
            } 

            Word(String word, boolean isVirus) { 
                  this(word, 10, isVirus); 
            } 

            Word(String word, int step, boolean isVirus) { 
                  this.word = word; 
                  this.step = step; 
                  this.isVirus = isVirus; 

                  if(isVirus) color = Color.RED; 
      
                  int strWidth = fm.stringWidth(word); 

                  x = (int)(Math.random() * SCREEN_WIDTH); 

                  if(x + strWidth >= SCREEN_WIDTH) 
                        x = SCREEN_WIDTH - strWidth; 
            } 

            public String toString() { 
                  return word; 
            } 
      } // end of class Word 

      class MyEventHandler extends WindowAdapter implements ActionListener { 
            public void actionPerformed(ActionEvent ae) { 
                  String input = tf.getText().trim(); 
                  tf.setText(""); 

                  System.out.println(input); 

                  if(!isPlaying) return; 

                  for(int i=0; i < words.size() ; i++) { 
                        Word tmp = (Word)words.get(i); 

                        if(input.equals(tmp.word)) { 
                              words.remove(i); 
                              score += input.length() * 50;                         
                              lbScore.setText("Score:"+score); 
                              Toolkit.getDefaultToolkit().beep(); 

                              if(curLevel!=MAX_LEVEL && levelUpCheck()) { 
                                    levelUp(); 
                              } else { 
                                    if(tmp.isVirus) { 
                                          new VirusThread(virusGrp, "virus").start();                                     
                                    } 
                              } 

                              break; 
                        } 
                  } // for 

                  repaint(); // 변화가 있을때 마다 호출해야 바로 바로 적용된다. 
            } 

            public void windowClosing(WindowEvent e) { 
                  e.getWindow().setVisible(false); 
                  e.getWindow().dispose(); 
                  System.exit(0); 
            } 
      } // class MyEventHandler 
}