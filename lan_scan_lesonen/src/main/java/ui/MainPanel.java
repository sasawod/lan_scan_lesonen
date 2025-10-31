/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ui;

import static com.sun.javafx.css.SizeUnits.PC;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.PopupMenu;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.JPanel;

/**
 *
 * @author Student
 */
public class MainPanel extends javax.swing.JPanel {

    /**
     * Creates new form MainPanel
     */
private pc pc;
    private JPanel[] panels;
    DefaultListModel dlm; // модель списка

    /**
     * Creates new form MainPanel
     */
    /**
     * Студент : Конструктор главной панели приложения
     * Инициализирует компоненты интерфейса и тестовые данные
     */
    public MainPanel() {
        initComponents();
        
        //  Создаем модель списка для хранения найденных устройств
        dlm = new DefaultListModel(); 
        dlm.clear();
        jList1.setModel(dlm);
        
        //  Добавляем тестовые данные для демонстрации
        dlm.add(0, "Comp6");
        dlm.add(1, "Comp5");
        dlm.add(2, "Comp4");
        dlm.add(3, "Comp3");
        dlm.add(4, "Comp2");
        dlm.add(5, "Comp1");
        
        //  Инициализируем прогресс-бар
        jProgressBar1.setValue(0);
        jProgressBar1.setString("Готов к сканированию");
        jLabel3.setText("Статус: Ожидание");
        
        System.out.println("Студент : Инициализация приложения завершена");
    }
    
/**
 * Метод сканирования сети с использованием SwingWorker
 *   - собственная реализация с прогресс-баром
 */
private void checkHost(String baseIp, int start, int end) {
    // Создаем SwingWorker для асинхронного сканирования
    SwingWorker<Void, ScanProgress> networkScanner = new SwingWorker<Void, ScanProgress>() {
        
        @Override
        protected Void doInBackground() throws Exception {
            //  Инициализация параметров сканирования
            final int timeoutMs = 200; // Оптимальный таймаут для быстрого сканирования
            int totalHosts = end - start + 1;
            int completedHosts = 0;
            int foundHosts = 0;
            
            //  Обновляем статус начала сканирования
            publish(new ScanProgress(0, "Начинаем сканирование сети...", 0));
            
            //  Создаем пул потоков для параллельного сканирования
            int threadCount = Math.min(20, totalHosts); // Максимум 20 потоков или количество хостов
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            System.out.println("Студент : Используем " + threadCount + " потоков для сканирования");
            AtomicInteger completedCounter = new AtomicInteger(0);
            AtomicInteger foundCounter = new AtomicInteger(0);
            
            //  Основной цикл сканирования с параллельным выполнением
            for (int i = start; i <= end; i++) {
                if (isCancelled()) {
                    break; // Проверяем отмену операции
                }
                
                final int hostNum = i;
                executor.submit(() -> {
                    String hostAddress = baseIp + "." + hostNum;
                    boolean isReachable = false;
                    
                    try {
                        //  Проверяем доступность хоста
                        isReachable = InetAddress.getByName(hostAddress).isReachable(timeoutMs);
                        
                        if (isReachable) {
                            int currentFound = foundCounter.incrementAndGet();
                            //  Добавляем найденный хост в список через EDT
                            SwingUtilities.invokeLater(() -> {
                                dlm.addElement(hostAddress);
                                System.out.println("Студент : Добавлен в список: " + hostAddress);
                            });
                        }
                        
                    } catch (Exception ex) {
                        //  Улучшенная обработка ошибок с детальным логированием
                        String errorMsg = "Ошибка при сканировании " + hostAddress + ": " + ex.getClass().getSimpleName();
                        System.err.println(errorMsg);
                    }
                    
                    //  Обновляем счетчик завершенных задач
                    int currentCompleted = completedCounter.incrementAndGet();
                    
                    //  Обновляем прогресс каждые 3 хоста или в конце для баланса скорости и отзывчивости
                    if (currentCompleted % 3 == 0 || currentCompleted == totalHosts) {
                        int progressPercent = (currentCompleted * 100) / totalHosts;
                        publish(new ScanProgress(progressPercent, 
                            "Сканирование: " + currentCompleted + "/" + totalHosts + 
                            " (найдено: " + foundCounter.get() + ")", foundCounter.get()));
                    }
                });
            }
            
            //  Ждем завершения всех задач
            executor.shutdown();
            try {
                executor.awaitTermination(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            //  Финальные значения
            completedHosts = completedCounter.get();
            foundHosts = foundCounter.get();
            
            //  Финальное обновление статуса
            publish(new ScanProgress(100, "Сканирование завершено! Найдено: " + foundHosts + " устройств", foundHosts));
            
            return null;
        }
        
        @Override
        protected void process(java.util.List<ScanProgress> chunks) {
            //  Обновляем UI в главном потоке
            for (ScanProgress progress : chunks) {
                jProgressBar1.setValue(progress.progress);
                jProgressBar1.setString(progress.message);
                jLabel3.setText("Статус: " + progress.message);
            }
        }
        
        @Override
        protected void done() {
            //  Завершаем сканирование с улучшенной обработкой ошибок
            try {
                get(); // Проверяем, не было ли исключений
                jLabel3.setText("Статус: Сканирование завершено успешно");
                jProgressBar1.setString("Готов к новому сканированию");
                System.out.println("Студент : Сканирование завершено успешно");
            } catch (Exception ex) {
                jLabel3.setText("Статус: Ошибка при сканировании");
                jProgressBar1.setString("Ошибка сканирования");
                
                //  Детальное логирование ошибок
                System.err.println("Критическая ошибка сканирования: " + ex.getClass().getSimpleName());
                System.err.println("Сообщение: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    };
    
    //  Запускаем сканирование
    networkScanner.execute();
}

/**
 * Вспомогательный класс для передачи данных о прогрессе
 *  Собственная реализация для SwingWorker
 */
private static class ScanProgress {
    final int progress;
    final String message;
    final int foundHosts;
    
    ScanProgress(int progress, String message, int foundHosts) {
        this.progress = progress;
        this.message = message;
        this.foundHosts = foundHosts;
    }
}


                   

                                         

            
        //checkHost()
                                            



    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jList1 = new javax.swing.JList<>();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jSpinner2 = new javax.swing.JSpinner();
        jSpinner3 = new javax.swing.JSpinner();
        jProgressBar1 = new javax.swing.JProgressBar();
        jLabel3 = new javax.swing.JLabel();

        jButton1.setText("show");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setText("scan");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jList1.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jScrollPane1.setViewportView(jList1);

        jPanel1.setLayout(new java.awt.BorderLayout());

        jLabel1.setText("диапазон");

        jTextField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField1ActionPerformed(evt);
            }
        });

        jLabel2.setText("интервал");
        
        jProgressBar1.setStringPainted(true);
        jProgressBar1.setString("Готов к сканированию");
        
        jLabel3.setText("Статус: Ожидание");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 206, Short.MAX_VALUE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addComponent(jProgressBar1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 131, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jSpinner2, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jSpinner3, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 69, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButton1)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 123, Short.MAX_VALUE)))
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 587, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(4, 4, 4)
                        .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jSpinner2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jSpinner3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jProgressBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 212, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jButton2)
                            .addComponent(jButton1))
                        .addGap(0, 123, Short.MAX_VALUE))))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // Студент : Обработчик кнопки "show" - отображает найденные компьютеры
        int countcomp = dlm.getSize();
        
        //  Отладочная информация о содержимом списка
        System.out.println("Студент : Размер списка: " + countcomp);
        for (int i = 0; i < countcomp; i++) {
            System.out.println("Элемент " + i + ": " + dlm.getElementAt(i));
        }
        
        createComp(countcomp);
        
        //  Логируем количество найденных устройств
        System.out.println("Отображаем " + countcomp + " найденных устройств");
    }//GEN-LAST:event_jButton1ActionPerformed


    
    public static void getNetworkIPS() {
    final byte[] ip;
    try {
        ip = InetAddress.getLocalHost().getAddress();
    } catch (Exception e) {
        return; 
    }
    
    for (int i = 1; i <= 254; i++) {
        final int j = i;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ip[3] = (byte) j;
                   
                    String host = "192.168.4" + "." + j;
                   
                    //InetAddress address = InetAddress.getByAddress(ip);
                    //String output = address.toString().substring(1);

                    if ( InetAddress.getByName(host).isReachable(200)) {
                        System.out.println(host + " is on the network");
                    } else {
                        System.out.println("Not Reachable: " + host);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    }
    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        // Студент : Обработчик кнопки "scan" - запускает сканирование сети
        String ipBase = jTextField1.getText().trim(); 
        int start = (Integer) jSpinner2.getValue();
        int end = (Integer) jSpinner3.getValue();
        
        //  Валидация входных данных с детальным логированием
        if (ipBase.isEmpty()) {
            jLabel3.setText("Статус: Ошибка - введите базовый IP");
            System.err.println("Студент : Ошибка валидации - пустой базовый IP");
            return;
        }
        
        if (start > end) {
            jLabel3.setText("Статус: Ошибка - начальный IP больше конечного");
            System.err.println("Студент : Ошибка валидации - некорректный диапазон: " + start + " > " + end);
            return;
        }
        
        if (start < 1 || end > 254) {
            jLabel3.setText("Статус: Ошибка - диапазон должен быть от 1 до 254");
            System.err.println("Студент : Ошибка валидации - выход за допустимый диапазон");
            return;
        }
        
        //  Запускаем сканирование с пользовательскими параметрами
        System.out.println("Студент : Запуск сканирования: " + ipBase + " от " + start + " до " + end);
        System.out.println("Студент : Очищаем список перед сканированием");
        dlm.clear(); // Очищаем список перед началом
        checkHost(ipBase, start, end);
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jTextField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField1ActionPerformed
/**
 * Студент : Метод создания визуальных компонентов для найденных компьютеров
 * Создает сетку из панелей с информацией о каждом найденном устройстве
 */
private void createComp(int quantityComps) {
    //  Инициализация массива панелей для каждого компьютера
    JPanel[] panels = new JPanel[quantityComps];
    jPanel1.removeAll();
    
    //  Создаем панель для каждого найденного устройства
    for (int i = 0; i < quantityComps; i++) {
        final JPanel dot = new JPanel();
        //  Создаем компонент PC с именем устройства
        pc = new pc(dlm.getElementAt(i).toString());
        dot.add(pc);
        panels[i] = dot;
    }
    
    //  Создаем основную панель с сеткой 3x3
    JPanel mainPanel = new JPanel();
    mainPanel.setLayout(new GridLayout(3, 3, 5, 5)); //  Отступы 5px между элементами

    //  Добавляем все панели устройств в основную панель
    for (int q = 0; q < panels.length; q++) {
        mainPanel.add(panels[q]);
    }
    
    //  Настраиваем layout и добавляем панель в интерфейс
    jPanel1.setLayout(new BorderLayout());
    jPanel1.add(mainPanel, BorderLayout.CENTER);
    jPanel1.revalidate();
    
    //  Логируем успешное создание компонентов
    System.out.println("Создано " + quantityComps + " визуальных компонентов устройств");
}



    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JList<String> jList1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JProgressBar jProgressBar1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSpinner jSpinner2;
    private javax.swing.JSpinner jSpinner3;
    private javax.swing.JTextField jTextField1;
    // End of variables declaration//GEN-END:variables
}
