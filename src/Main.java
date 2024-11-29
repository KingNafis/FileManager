import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Enhanced File Explorer");
            frame.setSize(800, 600);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // Root directory
            File rootFile = new File(System.getProperty("user.home"));
            DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(rootFile.getName());
            createFileTree(rootFile, rootNode);

            // JTree with custom icons
            JTree fileTree = new JTree(rootNode);
            fileTree.setCellRenderer(new FileTreeCellRenderer());
            fileTree.setRootVisible(true);

            // JList with custom icons
            DefaultListModel<File> listModel = new DefaultListModel<>();
            JList<File> fileList = new JList<>(listModel);
            fileList.setCellRenderer(new FileListCellRenderer());

            // Context menu for JList
            JPopupMenu contextMenu = new JPopupMenu();
            JMenuItem openItem = new JMenuItem("Open");
            JMenuItem deleteItem = new JMenuItem("Delete");
            JMenuItem renameItem = new JMenuItem("Rename");
            contextMenu.add(openItem);
            contextMenu.add(deleteItem);
            contextMenu.add(renameItem);

            // Add selection listener to JTree
            fileTree.addTreeSelectionListener(new TreeSelectionListener() {
                @Override
                public void valueChanged(TreeSelectionEvent e) {
                    DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) fileTree.getLastSelectedPathComponent();
                    if (selectedNode == null) return;

                    File selectedFile = new File(rootFile, getFullPath(selectedNode));
                    if (selectedFile.isDirectory()) {
                        loadDirectoryContents(selectedFile, listModel);
                    }
                }
            });

            // Add double-click listener to JList
            fileList.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        File selectedFile = fileList.getSelectedValue();
                        if (selectedFile != null && selectedFile.isFile()) {
                            openFile(selectedFile);
                        }
                    }
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        showContextMenu(e);
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        showContextMenu(e);
                    }
                }

                private void showContextMenu(MouseEvent e) {
                    fileList.setSelectedIndex(fileList.locationToIndex(e.getPoint()));
                    contextMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            });

            // Add actions to context menu
            openItem.addActionListener(e -> {
                File selectedFile = fileList.getSelectedValue();
                if (selectedFile != null) openFile(selectedFile);
            });

            deleteItem.addActionListener(e -> {
                File selectedFile = fileList.getSelectedValue();
                if (selectedFile != null && selectedFile.delete()) {
                    listModel.removeElement(selectedFile);
                }
            });

            renameItem.addActionListener(e -> {
                File selectedFile = fileList.getSelectedValue();
                if (selectedFile != null) {
                    String newName = JOptionPane.showInputDialog(frame, "Enter new name:", selectedFile.getName());
                    if (newName != null) {
                        File newFile = new File(selectedFile.getParent(), newName);
                        if (selectedFile.renameTo(newFile)) {
                            listModel.setElementAt(newFile, fileList.getSelectedIndex());
                        }
                    }
                }
            });

            // Add components to JSplitPane
            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(fileTree), new JScrollPane(fileList));
            splitPane.setDividerLocation(300);

            frame.add(splitPane);
            frame.setVisible(true);
        });
    }

    /**
     * Recursively creates the tree structure for directories.
     */
    private static void createFileTree(File file, DefaultMutableTreeNode node) {
        File[] files = file.listFiles();
        if (files == null) return;

        for (File f : files) {
            if (f.isDirectory()) {
                DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(f.getName());
                node.add(childNode);
                createFileTree(f, childNode);
            }
        }
    }

    /**
     * Constructs the full path of the selected node from the tree.
     */
    private static String getFullPath(DefaultMutableTreeNode node) {
        StringBuilder path = new StringBuilder(node.toString());
        while ((node = (DefaultMutableTreeNode) node.getParent()) != null) {
            path.insert(0, node.toString() + File.separator);
        }
        return path.toString();
    }

    /**
     * Loads directory contents into the JList on a separate thread.
     */
    private static void loadDirectoryContents(File directory, DefaultListModel<File> listModel) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            listModel.clear();
            File[] files = directory.listFiles();
            if (files != null) {
                SwingUtilities.invokeLater(() -> {
                    for (File file : files) {
                        listModel.addElement(file);
                    }
                });
            }
        });
        executor.shutdown();
    }

    /**
     * Opens a file with the system's default application.
     */
    private static void openFile(File file) {
        try {
            Desktop.getDesktop().open(file);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Unable to open file: " + ex.getMessage());
        }
    }

    /**
     * Custom renderer for JTree to display file icons.
     */
    static class FileTreeCellRenderer extends DefaultTreeCellRenderer {
        private final Icon folderIcon = UIManager.getIcon("FileView.directoryIcon");

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            setIcon(folderIcon);
            return this;
        }
    }

    /**
     * Custom renderer for JList to display file icons.
     */
    static class FileListCellRenderer extends DefaultListCellRenderer {
        private final Icon fileIcon = UIManager.getIcon("FileView.fileIcon");
        private final Icon folderIcon = UIManager.getIcon("FileView.directoryIcon");

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            File file = (File) value;
            setText(file.getName());
            setIcon(file.isDirectory() ? folderIcon : fileIcon);
            return this;
        }
    }
}
