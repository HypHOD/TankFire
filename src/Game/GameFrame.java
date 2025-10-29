package Game_old;

import Game.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameFrame extends Frame {
    int totalScore = 0;
    long startTimeMillis = 0L;
    // 游戏资源
    Image bg = loadImage("../img/duck.png");
    Image tank = loadImage("../img/tank.png");

    // 坦克状态
    boolean left = false;
    boolean right = false;
    boolean up = false;
    boolean down = false;
    int tankX = 300; // 坦克x坐标
    int tankY = 300; // 坦克y坐标
    int tankWidth = 100; // 坦克宽度
    int tankHeight = 50; // 坦克高度

    // 子弹管理
    List<Bullet> bulletList = new ArrayList<>();
    Random random = new Random(); // 用于生成随机值
    int bulletSpawnRate = 10; // 子弹生成概率（数值越大生成越慢）

    // 定义可点击区域
    private final Rectangle clickableArea = new Rectangle(20,50,100,100);
    private final String clickableAreaText = "Click here";

    public static void main(String[] args) {
        GameFrame frame = new GameFrame();
        frame.InitialFrame();
    }

    // 初始化窗口
    public void InitialFrame() {
        setTitle("TankF");
        setSize(800, 600);
        setLocationRelativeTo(null); // 居中显示
        setResizable(false); // 固定窗口大小
        startTimeMillis = System.currentTimeMillis();

        // 启动绘制线程
        new PaintThread().start();
        // 监听键盘
        addKeyListener(new KeyMonitor());
        // 监听窗口关闭
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int x = e.getX();
                int y = e.getY();
                if(clickableArea.contains(x, y)){
                    showInputDialog();
                }
            }
        });

        setVisible(true);
    }

    private void showInputDialog() {
        Dialog inputDialog = new Dialog(this,"Input cmd",true);
        inputDialog.setSize(300,300);
        inputDialog.setLocationRelativeTo(null);


        // show text
        Label tipLabel = new Label("Enter your command");
        tipLabel.setBounds(150,50,100,50);
        inputDialog.add(tipLabel);

        // input area
        TextField inputField = new TextField();
        inputField.setBounds(150,50,100,50);
        inputDialog.add(inputField);

        // ok button
        Button submitButton = new Button("Submit");
        submitButton.setBounds(150,100,100,50);
        submitButton.addActionListener(e -> {
            String command = inputField.getText().trim();
            handleInput(command);
            inputDialog.dispose();
        });
        inputDialog.add(submitButton);

        // close
        inputDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                inputDialog.dispose();
            }
        });
        inputDialog.setVisible(true);
    }

    private void handleInput(String command) {
        if(command.isEmpty()){ return; }
        switch (command){
            case "wear": showTipDialog("Input wear command");
            case "python": showTipDialog("Input python command");
            default: showTipDialog("Invalid command");
        }
    }

    private void showTipDialog(String message) {
        Dialog tipDialog = new Dialog(this, "提示", true);
        tipDialog.setSize(320, 180);
        tipDialog.setLocationRelativeTo(this);
        tipDialog.setLayout(null);

        Label tipLabel = new Label(message);
        tipLabel.setBounds(30, 40, 260, 60);
        tipLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        tipDialog.add(tipLabel);

        Button okBtn = new Button("确定");
        okBtn.setBounds(130, 120, 60, 30);
        okBtn.addActionListener(e -> tipDialog.dispose());
        tipDialog.add(okBtn);

        tipDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                tipDialog.dispose();
            }
        });

        tipDialog.setVisible(true);
    }

    // 子弹的大小
    Size small = new SmallSize();
    Size medium = new MediumSize();
    Size large = new LargeSize();
    // 子弹类
    class Bullet {
        int x; // 子弹x坐标
        int y; // 子弹y坐标
        int speed; // 子弹移动速度
        int width = 10; // 子弹宽度
        int height = 10; // 子弹高度
        String kind;
        double damage=1.0;

        // 子弹构造方法
        public Bullet(int startX, int startY, int speed, String kind, Size size) {
            this.x = startX;
            this.y = startY;
            this.speed = speed;
            this.kind = kind;
            switch (kind) {
                case "Cross": {
                    this.damage = new CrossBullet(size).calculateDamage();
                }
                break;
                case "Triangle": {
                    this.damage = new TriangleBullet(size).calculateDamage();
                }
                break;
                case "Dot": {
                    this.damage = new DotBullet(size).calculateDamage();
                }
                break;
                default:
                    break;
            }
        }

        // 更新子弹位置（向右移动）
        public void update() {
            this.x += this.speed;
        }
        // 绘制子弹
        // 根据大小和形状绘制
        public void draw(Graphics g) {
            g.setColor(Color.RED);
//            g.fillRect(x, y, (int) (width*this.damage), (int) (height*this.damage));
            // 根据种类绘制不同的子弹
            switch (kind) {
                case "Cross":{
                    // 画矩形
                    g.setColor(Color.BLACK);
                    g.fillRect(x, y, (int) (width*this.damage), (int) (height*this.damage));
                }
                break;
                case "Dot":{
                    // 画个点
                    g.setColor(Color.YELLOW);
                    g.drawRoundRect(x, y, (int) (width*this.damage), (int) (height*this.damage), 10, 10);
                }
                break;
                default:
                    break;
            }
        }

        // 判断子弹是否超出屏幕
        public boolean isOutOfScreen() {
            return this.x > GameFrame.this.getWidth();
        }
    }

    // 绘制线程
    class PaintThread extends Thread {
        @Override
        public void run() {
            while (true) {
                // 随机生成新子弹（从屏幕左侧）
                spawnRandomBullet();

                // 更新坦克位置
                updateTankPos();

                // 更新所有子弹位置并清理超出屏幕的子弹
                updateBullets();

                // 重绘画面
                repaint();

                // 控制帧率
                try {
                    Thread.sleep(33); // 约30帧/秒
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        // 随机生成子弹（从屏幕左侧）
        private void spawnRandomBullet() {
            // 随机概率生成子弹，控制生成频率
            if (random.nextInt(bulletSpawnRate) == 0) {
                // 子弹从屏幕左侧生成（x=0）
                int startX = 0;
                // 子弹y坐标在屏幕高度范围内随机
                int startY = random.nextInt(getHeight() - 10); // 10是子弹高度
                // 子弹速度随机（2-8像素/帧）
                int speed = random.nextInt(7) + 2; // [2,8]范围

                // 添加新子弹到列表
                // 生成随机大小和形状的子弹
//                bulletList.add(new Bullet(startX, startY, speed));
                switch (new Random().nextInt(7)) {
                    case 0: break;
                    case 1:
                        bulletList.add(new Bullet(startX, startY, speed, "Cross", small));
                        break;
                    case 2:
                        bulletList.add(new Bullet(startX, startY, speed, "Cross", medium));
                        break;
                    case 3:
                        bulletList.add(new Bullet(startX, startY, speed, "Cross", large));
                        break;
                    case 4:
                        bulletList.add(new Bullet(startX, startY, speed, "Dot", small));
                        break;
                    case 5:
                        bulletList.add(new Bullet(startX, startY, speed, "Dot", medium));
                        break;
                    case 6:
                        bulletList.add(new Bullet(startX, startY, speed, "Dot", large));
                        break;

                }
//                bulletList.add(new Bullet(startX, startY, speed, "Cross", small));
            }
        }

        // 更新坦克位置
        private void updateTankPos() {
            if (left && tankX > 0) tankX -= 5;
            if (right && tankX < getWidth() - tankWidth) tankX += 5;
            if (up && tankY > 0) tankY -= 5;
            if (down && tankY < getHeight() - tankHeight) tankY += 5;
        }

        // 更新所有子弹
        private void updateBullets() {
            for (int i = 0; i < bulletList.size(); i++) {
                Bullet bullet = bulletList.get(i);
                bullet.update(); // 移动子弹
                // 碰撞检测：子弹与坦克相交则加分并移除子弹
                Rectangle bulletRect = new Rectangle(bullet.x, bullet.y, bullet.width, bullet.height);
                Rectangle tankRect = new Rectangle(tankX, tankY, tankWidth, tankHeight);
                if (bulletRect.intersects(tankRect)) {
                    totalScore += (int) bulletList.get(i).damage;
                    bulletList.remove(i);
//                    totalScore += 1;
                    i--;
                    continue;
                }
                if (bullet.isOutOfScreen()) {
                    bulletList.remove(i); // 移除超出屏幕的子弹
                    i--; // 调整索引，避免漏检
                }
            }
        }
    }

    // 双缓冲解决闪屏
    private Image offScreenImage = null;

    @Override
    public void update(Graphics g) {
        if (offScreenImage == null) {
            offScreenImage = createImage(getWidth(), getHeight());
        }
        Graphics offG = offScreenImage.getGraphics();
        offG.setColor(getBackground());
        offG.fillRect(0, 0, getWidth(), getHeight());
        paint(offG);
        g.drawImage(offScreenImage, 0, 0, null);
        offG.dispose();
    }

    // 键盘监听
    class KeyMonitor extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_LEFT:
                    left = true;
                    break;
                case KeyEvent.VK_RIGHT:
                    right = true;
                    break;
                case KeyEvent.VK_UP:
                    up = true;
                    break;
                case KeyEvent.VK_DOWN:
                    down = true;
                    break;
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_LEFT:
                    left = false;
                    break;
                case KeyEvent.VK_RIGHT:
                    right = false;
                    break;
                case KeyEvent.VK_UP:
                    up = false;
                    break;
                case KeyEvent.VK_DOWN:
                    down = false;
                    break;
            }
        }
    }

    // 绘制游戏元素
    @Override
    public void paint(Graphics g) {
        // 绘制背景
        g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
        // 绘制可点击区域
        Color keepColor = g.getColor();

        g.setColor(new Color(0, 0, 0, 180));
        g.fillRoundRect(
                clickableArea.x, clickableArea.y,
                clickableArea.width, clickableArea.height,
                8, 8 // 圆角半径
        );
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        FontRenderContext frc = ((Graphics2D) g).getFontRenderContext();
        Rectangle2D textRect = g.getFont().getStringBounds(clickableAreaText, frc);
        int textX1 = clickableArea.x + (int)((clickableArea.width - textRect.getWidth())/2);
        int textY1 = clickableArea.y + (int)((clickableArea.height + textRect.getHeight())/2) - 3;
        g.drawString(clickableAreaText, textX1, textY1);
        g.setColor(keepColor);

        // 绘制坦克
        g.drawImage(tank, tankX, tankY, tankWidth, tankHeight, this);
        // 绘制所有子弹
        for (Bullet bullet : bulletList) {
            bullet.draw(g);
        }
        // 中间下方时间与分数（同一行，Score 在 Time 右侧）
        g.setFont(new Font("Arial", Font.BOLD, 18));
        long elapsedSec = Math.max(0, (System.currentTimeMillis() - startTimeMillis) / 1000);
        long mm = elapsedSec / 60;
        long ss = elapsedSec % 60;
        String leftTimeText = String.format("Time: %02d:%02d", mm, ss);
        String leftScoreText = "  |  Score: " + totalScore;
        
        // 计算居中位置
        int timeWidth = g.getFontMetrics().stringWidth(leftTimeText);
        int scoreWidth = g.getFontMetrics().stringWidth(leftScoreText);
        int totalWidth = timeWidth + scoreWidth;
        int baseX = (getWidth() - totalWidth) / 2; // 水平居中
        int baseY = getHeight() - 30; // 距离底部30像素
        
        // 绘制半透明背景
        Color old = g.getColor();
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRoundRect(baseX - 10, baseY - 18, totalWidth + 20, 25, 5, 5);
        
        // 绘制文字（黄色，更醒目）
        g.setColor(Color.YELLOW);
        g.drawString(leftTimeText, baseX, baseY);
        g.drawString(leftScoreText, baseX + timeWidth, baseY);
        g.setColor(old);
        // 右上角分数
        g.setFont(new Font("Arial", Font.BOLD, 18));
        String scoreText = "Score: " + totalScore;
        int textWidth = g.getFontMetrics().stringWidth(scoreText);
        int textX = getWidth() - textWidth - 12;
        int textY = 24;
        // 背景遮罩增强可读性
        Color oldColor = g.getColor();
        g.setColor(new Color(0,0,0,120));
        g.fillRoundRect(textX - 8, textY - 20, textWidth + 16, 26, 8, 8);
        // 阴影 + 文字
        g.setColor(Color.BLACK);
        g.drawString(scoreText, textX + 1, textY + 1);
        g.setColor(Color.WHITE);
        g.drawString(scoreText, textX, textY);
        g.setColor(oldColor);
        // 绘制边框
        g.setColor(Color.BLACK);
        g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
    }

    // 图像加载工具
    public Image loadImage(String imagePath) {
        URL url = GameFrame.class.getResource(imagePath);
        if (url != null) {
            try {
                return ImageIO.read(url);
            } catch (Exception e) {
                throw new RuntimeException("图像加载失败：" + imagePath, e);
            }
        }
        System.err.println("图像路径不存在：" + imagePath);
        System.exit(0);
        return null;
    }
}