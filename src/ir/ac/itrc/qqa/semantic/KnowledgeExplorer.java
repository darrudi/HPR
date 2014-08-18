package ir.ac.itrc.qqa.semantic;

import ir.ac.itrc.qqa.semantic.enums.ConceptType;
import ir.ac.itrc.qqa.semantic.enums.ExecutionMode;
import ir.ac.itrc.qqa.semantic.enums.GraphLayouts;
import ir.ac.itrc.qqa.semantic.enums.LexicalType;
import ir.ac.itrc.qqa.semantic.enums.POS;
import ir.ac.itrc.qqa.semantic.enums.PreprocessorType;
import ir.ac.itrc.qqa.semantic.enums.StringMatch;
import ir.ac.itrc.qqa.semantic.kb.KnowledgeBase;
import ir.ac.itrc.qqa.semantic.kb.Node;
import ir.ac.itrc.qqa.semantic.reasoning.PlausibleAnswer;
import ir.ac.itrc.qqa.semantic.reasoning.PlausibleQuestion;
import ir.ac.itrc.qqa.semantic.reasoning.SemanticReasoner;
import ir.ac.itrc.qqa.semantic.util.Common;
import ir.ac.itrc.qqa.semantic.util.MyError;

import java.awt.ComponentOrientation;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Shape;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JList;

import java.awt.SystemColor;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.JSeparator;
import javax.swing.JButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Color;

import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JPopupMenu;

import java.awt.Component;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;

import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.functors.ConstantTransformer;

import edu.uci.ics.jung.algorithms.layout.GraphElementAccessor;
import edu.uci.ics.jung.algorithms.layout.KKLayout;
import edu.uci.ics.jung.algorithms.layout.CircleLayout;
import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.ISOMLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.SpringLayout;
import edu.uci.ics.jung.algorithms.layout.SpringLayout2;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.AbstractPopupGraphMousePlugin;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.AbstractVertexShapeTransformer;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.picking.PickedInfo;
import edu.uci.ics.jung.visualization.renderers.Renderer;

import javax.swing.JCheckBox;

/**
 * KnowledgeExplorer provides an easy interface for knowledge engineers to search and browse concepts and relations.
 * This class implements the main form using Swing.
 * Because most of the code in this class implement usual features there is no other documentation available.
 * 
 * @author Ehsan Darrudi
 *
 */
public class KnowledgeExplorer
{
	private JFrame frame;
	
	private JTextField textWord;
	private JTextArea textArea;
	private JTextField textReferent;
	private JTextField textArgument;
	private JTextField textDescriptor;
	private JTextField textContext;
	
	private JList listSenses;
	
	private JMenuItem menuLoadKb;
	private JMenuItem menuShowDescriptorTypes;
	
	private JButton btnSubstringSearch;
	private JButton btnPrefixSearch;
	private JButton btnExactSearch;
	private JButton btnHistoryPrev;
	private JButton btnHistoryNext;
	
	private JComboBox<String> comboPos;
	private JComboBox<String> comboCat;
	private JComboBox<Pov> comboPov;
	
	JSpinner spinnerMaxReasonongDepth;
	JSpinner spinnerMaxReasonongAnswers;
	
	private JLabel labelRetrieved;
	
	JScrollPane scrollSenses;
	
	private DefaultListModel<Node> modelList = new DefaultListModel<Node>();
	
	private boolean isKbInitialized = false;
	
	private KnowledgeBase _kb = new KnowledgeBase();	
	private SemanticReasoner _re = new SemanticReasoner(_kb, ExecutionMode.RELEASE);
	
	HistoryItem currentHistoryItem;
	
	Iterator iteratorConcept;
	int counterConcept;
	
	private Node currentVisualNode = null;
	GraphLayouts currentGraphLayout = GraphLayouts.KK;
	
	JFrame frameGraph = new JFrame("بازنمایی گرافیکی");
	
	ArrayList<PlausibleAnswer> _lastAnswers = null;
	PlausibleQuestion _lastPlausibleQuestion = null;
	String _lastNaturalQuestion = null;
	private JTextField textMiniSearch;
	
	
	public class HistoryItem
	{
		public String word;
		public DefaultListModel modelSenses;
		public int indexSenses;
		public String text;
		public int indexPos;
		public int indexCat;
		
		public HistoryItem next = null;
		public HistoryItem prev = null;
	}
	
	// to truncate long node names in the list (overriding the default Node.toString)
	class MyCellRenderer implements ListCellRenderer<Node> 
	{
		protected DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();

		  public Component getListCellRendererComponent(JList list, Node node, int index, boolean isSelected, boolean cellHasFocus) 
		  {
			  JLabel renderer = (JLabel)defaultRenderer.getListCellRendererComponent(list, node, index, isSelected, cellHasFocus);
			  
			  String text = node.toString();
			  
			  if (text.length() > 35)
				  text = text.substring(0, 35) + "...";
			  
			  renderer.setText(text);
			  return renderer;
		  }
	}
	
	public enum MethodType
	{
		SYNONYM,
		ANTONYM
	}
	
	public int loadKb(String path)
	{
		_kb = new KnowledgeBase();
		_re = new SemanticReasoner(_kb, ExecutionMode.RELEASE);
		
		int loaded = _kb.importKb(path);
		
		isKbInitialized = true;
		
		currentHistoryItem = null;
		
		iteratorConcept = _kb.getNodesSetIterator().iterator();
		counterConcept = 0;
		
		return loaded;
	}

	/**
	 * Launch the application.
	 */
	public static void main(String[] args)
	{
		//$hide>>$
	    try {
	        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
	    } catch(Exception e) {
	        System.out.println("Error setting native LAF: " + e);
	    }
	    //$hide<<$
		
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				try
				{
					KnowledgeExplorer window = new KnowledgeExplorer();
					window.frame.setVisible(true);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public KnowledgeExplorer()
	{
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize()
	{
		frame = new JFrame();
		frame.setBounds(100, 100, 1003, 674);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setResizable(false);
		frame.setLocationRelativeTo(null);
		
		JMenuBar menuBar = new JMenuBar();
		menuBar.setBorderPainted(false);
		menuBar.setFont(new Font("Tahoma", Font.PLAIN, 10));
		frame.setJMenuBar(menuBar);
		
		JMenu mnNewMenu = new JMenu("فایل");
		mnNewMenu.setFont(new Font("Tahoma", Font.PLAIN, 12));
		menuBar.add(mnNewMenu);
		
		JMenuItem mntmNewMenuItem_1 = new JMenuItem("درباره این برنامه");
		mntmNewMenuItem_1.setIcon(new ImageIcon(KnowledgeExplorer.class.getResource("/javax/swing/plaf/basic/icons/JavaCup16.png")));
		mntmNewMenuItem_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) 
			{
				textArea.setText("");
				print("مرورگر دانش\r\nگروه معنایی\r\nسامانه پرسش و پاسخ قرآنی\r\nمرکز تحقیقات مخابرات ایران\r\nاحسان درودی\r\ndarrudi@csri.ac.ir\r\nاسفند نود و یک");
			}
		});
		mntmNewMenuItem_1.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) 
			{
				
			}
		});
		mntmNewMenuItem_1.setFont(new Font("Tahoma", Font.PLAIN, 12));
		mnNewMenu.add(mntmNewMenuItem_1);
		
		JMenuItem menuItem = new JMenuItem("خروج");
		menuItem.setIcon(new ImageIcon(KnowledgeExplorer.class.getResource("/javax/swing/plaf/metal/icons/ocean/minimize.gif")));
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) 
			{
				System.exit(0);
			}
		});
		menuItem.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) 
			{
				System.exit(0);
			}
		});
		menuItem.setFont(new Font("Tahoma", Font.PLAIN, 12));
		mnNewMenu.add(menuItem);
		
		JMenu menu = new JMenu("پایگاه دانش");
		menu.setFont(new Font("Tahoma", Font.PLAIN, 12));
		menuBar.add(menu);
		
		menuLoadKb = new JMenuItem("آمار بانک های داده بار شده");
		menuLoadKb.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) 
			{
				showKbStats(true);
			}
		});
		
		JMenuItem menuItem_5 = new JMenuItem("بار کردن پایگاه دانش دلخواه");
		menuItem_5.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) 
			{
				//Create a file chooser
				final JFileChooser fc = new JFileChooser();

				//In response to a button click:
				int returnVal = fc.showOpenDialog(frame);
				
				if (returnVal == JFileChooser.APPROVE_OPTION) 
				{
					final File file = fc.getSelectedFile();
					
					textArea.setText("");
					modelList = new DefaultListModel<Node>();
					listSenses.setModel(modelList);
					labelRetrieved.setText("0");
					
					print("بار کردن پایگاه دانش " + file.getName() + " ... ");
					
					EventQueue.invokeLater(new Runnable()
					{
						public void run()
						{
							Long start = System.currentTimeMillis();
							
							int relations = loadKb(file.getPath());
							
							Long end = System.currentTimeMillis();
							
							print("");
							print("تعداد «" + relations + "» رابطه در مدت زمان «" + (end - start)/1000 + "» ثانیه در حافظه بار شد.");
							
							showKbStats(false);
						}
					});					
		        }
			}
		});
		

		menuItem_5.setIcon(new ImageIcon(KnowledgeExplorer.class.getResource("/com/sun/java/swing/plaf/windows/icons/Directory.gif")));
		menuItem_5.setFont(new Font("Tahoma", Font.PLAIN, 12));
		menu.add(menuItem_5);
		
		JMenuItem mntmNewMenuItem_5 = new JMenuItem("ذخیره پایگاه دانش جاری");
		mntmNewMenuItem_5.setEnabled(false);
		mntmNewMenuItem_5.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) 
			{
				textArea.setText("");
				
				print("ذخیره سازی پایگاه دانش جاری ...");
				print("hpr-kb-dump.txt");
				
				_kb.exportKb("hpr-kb-dump.txt");
				
				print("انجام شد");
			}
		});
		mntmNewMenuItem_5.setFont(new Font("Tahoma", Font.PLAIN, 12));
		menu.add(mntmNewMenuItem_5);
		menuLoadKb.setFont(new Font("Tahoma", Font.PLAIN, 12));
		menu.add(menuLoadKb);
		
		JMenu menu_2 = new JMenu("نمایش");
		menu_2.setIcon(new ImageIcon(KnowledgeExplorer.class.getResource("/javax/swing/plaf/metal/icons/ocean/computer.gif")));
		menu_2.setFont(new Font("Tahoma", Font.PLAIN, 12));
		menu.add(menu_2);
		
		menuShowDescriptorTypes = new JMenuItem("انواع روابط مورد پشتیبانی");
		menuShowDescriptorTypes.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) 
			{
				print("--------------- روابط و تعداد آنها ---------------");
				
				String output = _kb.getDescriptorsAsText();
				
				print(output);
			}
		});
		menuShowDescriptorTypes.setFont(new Font("Tahoma", Font.PLAIN, 12));
		menu_2.add(menuShowDescriptorTypes);
		
		JMenu menu_1 = new JMenu("استدلال");
		menu_1.setFont(new Font("Tahoma", Font.PLAIN, 12));
		menuBar.add(menu_1);
		
		JMenuItem menuItem_3 = new JMenuItem("نمایش توجیه جواب");
		menuItem_3.setEnabled(false);
		menuItem_3.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) 
			{
				showAnswerJustifications();
			}
		});
		
		menuItem_3.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_J, java.awt.Event.CTRL_MASK));
		menuItem_3.setFont(new Font("Tahoma", Font.PLAIN, 12));
		menu_1.add(menuItem_3);
		
		JMenuItem menuItem_4 = new JMenuItem("نمایش روند استدلال");
		menuItem_4.setEnabled(false);
		menuItem_4.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) 
			{
				
			}
		});
		menuItem_4.setFont(new Font("Tahoma", Font.PLAIN, 12));
		menu_1.add(menuItem_4);
		frame.getContentPane().setLayout(null);
		
		JLabel label = new JLabel("کلمه:");
		label.setHorizontalAlignment(SwingConstants.RIGHT);
		label.setFont(new Font("Tahoma", Font.PLAIN, 12));
		label.setBounds(923, 32, 46, 14);
		frame.getContentPane().add(label);
		
		textWord = new JTextField();
		textWord.setFont(new Font("Tahoma", Font.PLAIN, 12));
		textWord.setHorizontalAlignment(SwingConstants.RIGHT);
		textWord.setBounds(738, 29, 179, 20);
		frame.getContentPane().add(textWord);
		textWord.setColumns(10);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(22, 92, 692, 352);
		frame.getContentPane().add(scrollPane);
		
		textArea = new JTextArea();
		textArea.setWrapStyleWord(true);
		textArea.setEditable(false);
		scrollPane.setViewportView(textArea);		
		textArea.setBackground(SystemColor.info);
		textArea.setToolTipText("");
		textArea.setTabSize(4);
		textArea.setFont(new Font("Tahoma", Font.PLAIN, 12));
		textArea.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
		
		JPopupMenu popupMenu = new JPopupMenu();
		popupMenu.setFont(new Font("Tahoma", Font.PLAIN, 12));
		addPopup(textArea, popupMenu);
		
		JMenuItem menuItem_2 = new JMenuItem("جستجو برای مفاهیم با انطباق کامل با کلمه انتخاب شده");
		menuItem_2.setEnabled(false);
		menuItem_2.setIcon(new ImageIcon(KnowledgeExplorer.class.getResource("/javax/swing/plaf/metal/icons/ocean/iconify.gif")));
		menuItem_2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) 
			{
				if (!checkKbInit())
					return;
				
				String str = textArea.getSelectedText().trim();
				
				textWord.setText(str);

				Node concept = _kb.findConcept(str);
				
				modelList = new DefaultListModel<Node>();
				
				if (concept == null)
				{
					textArea.setText("موردی یافت نشد");
					
					updateModel(modelList);
					
					return;
				}
				
				textArea.setText("");				
				
				modelList.addElement(concept);
				
				updateModel(modelList);
								
				listSenses.setSelectedIndex(0);
				showSelectedConceptInfo(true);
			}
		});
		menuItem_2.setFont(new Font("Tahoma", Font.PLAIN, 12));
		popupMenu.add(menuItem_2);
		
		JMenuItem mntmNewMenuItem = new JMenuItem("جستجو برای معانی شروع شده با کلمه انتخاب شده");
		mntmNewMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) 
			{
				textWord.setText(textArea.getSelectedText());
				btnPrefixSearch.doClick();
				
				listSenses.setSelectedIndex(0);
				showSelectedConceptInfo(true);
			}
		});
		
		JMenuItem mntmNewMenuItem_3 = new JMenuItem("جستجو برای معانی با انطباق کامل با کلمه انتخاب شده");
		mntmNewMenuItem_3.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) 
			{
				textWord.setText(textArea.getSelectedText());
				btnExactSearch.doClick();
				
				listSenses.setSelectedIndex(0);
				showSelectedConceptInfo(true);
			}
		});
		
		JSeparator separator = new JSeparator();
		popupMenu.add(separator);
		mntmNewMenuItem_3.setFont(new Font("Tahoma", Font.PLAIN, 12));
		popupMenu.add(mntmNewMenuItem_3);
		mntmNewMenuItem.setFont(new Font("Tahoma", Font.PLAIN, 12));
		popupMenu.add(mntmNewMenuItem);
		
		JMenuItem mntmNewMenuItem_2 = new JMenuItem("جستجو برای معانی شامل کلمه انتخاب شده");
		mntmNewMenuItem_2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) 
			{
				textWord.setText(textArea.getSelectedText());
				btnSubstringSearch.doClick();
				
				listSenses.setSelectedIndex(0);
				showSelectedConceptInfo(true);
			}
		});
		mntmNewMenuItem_2.setFont(new Font("Tahoma", Font.PLAIN, 12));
		popupMenu.add(mntmNewMenuItem_2);
		
		btnSubstringSearch = new JButton("شامل");
		btnSubstringSearch.setToolTipText("برای یک کلمه همه معانی شامل آن را لیست میکند");
		btnSubstringSearch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) 
			{
				searchSensesFromSubstr(StringMatch.SUBSTRING);
			}
		});
		
		btnSubstringSearch.setFont(new Font("Tahoma", Font.PLAIN, 12));
		btnSubstringSearch.setBounds(651, 28, 63, 23);
		frame.getContentPane().add(btnSubstringSearch);
		
		scrollSenses = new JScrollPane();
		scrollSenses.setBounds(738, 92, 233, 352);
		frame.getContentPane().add(scrollSenses);
		
		listSenses = new JList<Node>();
		ListCellRenderer<Node> renderer = new MyCellRenderer();
		listSenses.setCellRenderer(renderer);		
		listSenses.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent arg0) {
                if (!arg0.getValueIsAdjusting()) 
                {
                	showSelectedConceptInfo(true);
                }
            }
        });

		
		scrollSenses.setViewportView(listSenses);
		scrollSenses.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
	
		listSenses.setFont(new Font("Tahoma", Font.PLAIN, 12));

		
		listSenses.setModel(modelList);		
		listSenses.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
		
		//--------------------------------------
		
		textReferent = new JTextField();
		textReferent.setFont(new Font("Tahoma", Font.PLAIN, 12));
		textReferent.setHorizontalAlignment(SwingConstants.RIGHT);
		textReferent.setBounds(570, 542, 125, 25);
		frame.getContentPane().add(textReferent);
		textReferent.setColumns(10);
		
		textArgument = new JTextField();
		textArgument.setFont(new Font("Tahoma", Font.PLAIN, 12));
		textArgument.setHorizontalAlignment(SwingConstants.RIGHT);
		textArgument.setBounds(705, 542, 125, 25);
		frame.getContentPane().add(textArgument);
		textArgument.setColumns(10);
		
		textDescriptor = new JTextField();
		textDescriptor.setFont(new Font("Tahoma", Font.PLAIN, 12));
		textDescriptor.setHorizontalAlignment(SwingConstants.RIGHT);
		textDescriptor.setBounds(840, 542, 125, 25);
		frame.getContentPane().add(textDescriptor);
		textDescriptor.setColumns(10);
		
		JButton btnQa = new JButton("بازیابی");
		btnQa.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) 
			{
				//retrieveReferences();
			}
		});
		btnQa.setFont(new Font("Tahoma", Font.PLAIN, 12));
		btnQa.setBounds(311, 571, 121, 27);
		frame.getContentPane().add(btnQa);
		
		JButton btnCls = new JButton("پاک کن");
		btnCls.setForeground(new Color(128, 0, 0));
		btnCls.setFont(new Font("Tahoma", Font.PLAIN, 12));
		btnCls.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) 
			{
				textArea.setText("");
			}
		});
		
		btnCls.setBounds(22, 28, 90, 53);
		frame.getContentPane().add(btnCls);
		
		JLabel lblProperty = new JLabel("مقصد:");
		lblProperty.setEnabled(false);
		lblProperty.setFont(new Font("Tahoma", Font.PLAIN, 12));
		lblProperty.setHorizontalAlignment(SwingConstants.RIGHT);
		lblProperty.setBounds(649, 524, 46, 14);
		frame.getContentPane().add(lblProperty);
		
		JLabel lblObject = new JLabel("مبداء:");
		lblObject.setEnabled(false);
		lblObject.setFont(new Font("Tahoma", Font.PLAIN, 12));
		lblObject.setHorizontalAlignment(SwingConstants.RIGHT);
		lblObject.setBounds(784, 524, 46, 14);
		frame.getContentPane().add(lblObject);
		
		JLabel lblValue = new JLabel("رابطه:");
		lblValue.setEnabled(false);
		lblValue.setFont(new Font("Tahoma", Font.PLAIN, 12));
		lblValue.setHorizontalAlignment(SwingConstants.RIGHT);
		lblValue.setBounds(919, 524, 46, 14);
		frame.getContentPane().add(lblValue);
		
		spinnerMaxReasonongDepth = new JSpinner();
		spinnerMaxReasonongDepth.setFont(new Font("Tahoma", Font.PLAIN, 12));
		spinnerMaxReasonongDepth.setModel(new SpinnerNumberModel(new Integer(9), null, null, new Integer(1)));
		spinnerMaxReasonongDepth.setBounds(233, 578, 68, 20);
		frame.getContentPane().add(spinnerMaxReasonongDepth);
		
		JLabel label_1 = new JLabel("عمق استدلال:");
		label_1.setFont(new Font("Tahoma", Font.PLAIN, 12));
		label_1.setHorizontalAlignment(SwingConstants.RIGHT);
		label_1.setBounds(223, 560, 78, 14);
		frame.getContentPane().add(label_1);
		
		JLabel label_2 = new JLabel("حداکثر جواب:");
		label_2.setFont(new Font("Tahoma", Font.PLAIN, 12));
		label_2.setHorizontalAlignment(SwingConstants.RIGHT);
		label_2.setBounds(144, 560, 69, 14);
		frame.getContentPane().add(label_2);
		
		spinnerMaxReasonongAnswers = new JSpinner();
		spinnerMaxReasonongAnswers.setFont(new Font("Tahoma", Font.PLAIN, 12));
		spinnerMaxReasonongAnswers.setModel(new SpinnerNumberModel(new Integer(5), null, null, new Integer(1)));
		spinnerMaxReasonongAnswers.setBounds(144, 578, 69, 20);
		frame.getContentPane().add(spinnerMaxReasonongAnswers);
		
		JSeparator separator_1 = new JSeparator();
		separator_1.setBounds(22, 477, 951, 7);
		frame.getContentPane().add(separator_1);
		
		JLabel lblKe = new JLabel("مرورگر دانش");
		lblKe.setForeground(Color.LIGHT_GRAY);
		lblKe.setFont(new Font("B Titr", Font.PLAIN, 34));
		lblKe.setBounds(24, 496, 184, 57);
		frame.getContentPane().add(lblKe);
		
		JLabel lblQqaKnowledgeExplorer = new JLabel("مرورگر دانش");
		lblQqaKnowledgeExplorer.setForeground(Color.WHITE);
		lblQqaKnowledgeExplorer.setFont(new Font("B Titr", Font.PLAIN, 34));
		lblQqaKnowledgeExplorer.setBounds(22, 495, 186, 58);
		frame.getContentPane().add(lblQqaKnowledgeExplorer);
		
		comboPos = new JComboBox();
		comboPos.setFont(new Font("Tahoma", Font.PLAIN, 12));
		
		comboPos.setModel(new DefaultComboBoxModel(new String[] {"همه مقوله ها", "اسم", "فعل", "صفت", "قید", "صفت اقماری"}));
		comboPos.setSelectedIndex(0);
		comboPos.setBounds(857, 61, 112, 20);
		frame.getContentPane().add(comboPos);
		
		JButton button = new JButton("مترادف");
		button.setToolTipText("");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) 
			{
				if (!checkKbInit())
					return;			
								
				modelList = new DefaultListModel<Node>();
				
				EventQueue.invokeLater(new Runnable()
				{
					public void run()
					{
						findSynonymAntonym(MethodType.SYNONYM);
					}
				});
			}
		});
		button.setFont(new Font("Tahoma", Font.PLAIN, 12));
		button.setBounds(173, 28, 69, 23);
		frame.getContentPane().add(button);
		
		btnExactSearch = new JButton("دقیق");
		btnExactSearch.setToolTipText("برای یک کلمه همه معانی که انطباق دقیق با آن دارند را لیست می کند");
		btnExactSearch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) 
			{
				searchSensesFromSubstr(StringMatch.EXACT);
			}
		});
		btnExactSearch.setFont(new Font("Tahoma", Font.PLAIN, 12));
		btnExactSearch.setBounds(506, 28, 63, 23);
		frame.getContentPane().add(btnExactSearch);
		
		JButton button_2 = new JButton("متضاد");
		button_2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) 
			{
				if (!checkKbInit())
					return;			
								
				textArea.setText("");
				modelList = new DefaultListModel<Node>();
				
				EventQueue.invokeLater(new Runnable()
				{
					public void run()
					{
						findSynonymAntonym(MethodType.ANTONYM);
					}
				});
			}
		});
		button_2.setFont(new Font("Tahoma", Font.PLAIN, 12));
		button_2.setBounds(173, 58, 69, 23);
		frame.getContentPane().add(button_2);
		
		btnPrefixSearch = new JButton("شروع با");
		btnPrefixSearch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) 
			{
				searchSensesFromSubstr(StringMatch.PREFIX);
			}
		});
		btnPrefixSearch.setToolTipText("برای یک کلمه همه معانی شامل آن را لیست میکند");
		btnPrefixSearch.setFont(new Font("Tahoma", Font.PLAIN, 12));
		btnPrefixSearch.setBounds(570, 28, 80, 23);
		frame.getContentPane().add(btnPrefixSearch);
		
		JLabel label_5 = new JLabel("نسخه 5.0");
		label_5.setForeground(Color.LIGHT_GRAY);
		label_5.setFont(new Font("B Koodak", Font.PLAIN, 26));
		label_5.setBounds(24, 548, 110, 29);
		frame.getContentPane().add(label_5);
		
		JLabel label_6 = new JLabel("نسخه 5.0");
		label_6.setForeground(Color.WHITE);
		label_6.setFont(new Font("B Koodak", Font.PLAIN, 26));
		label_6.setBounds(22, 547, 112, 30);
		frame.getContentPane().add(label_6);
		
		textArea.getCaret().setVisible(true);
		
		btnHistoryPrev = new JButton("");
		btnHistoryPrev.setEnabled(false);
		btnHistoryPrev.setToolTipText("مورد قبلی در تاریخچه");
		btnHistoryPrev.setIcon(new ImageIcon(KnowledgeExplorer.class.getResource("/javax/swing/plaf/metal/icons/sortDown.png")));
		btnHistoryPrev.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) 
			{
				if (currentHistoryItem != null && currentHistoryItem.prev != null)
				{
					currentHistoryItem = currentHistoryItem.prev;
					
					textWord.setText(currentHistoryItem.word);
					comboPos.setSelectedIndex(currentHistoryItem.indexPos);
					comboCat.setSelectedIndex(currentHistoryItem.indexCat);
					updateModel(currentHistoryItem.modelSenses);
					listSenses.setSelectedIndex(currentHistoryItem.indexSenses);

					showSelectedConceptInfo(false);
					
					btnHistoryNext.setEnabled(true);
					
					if (currentHistoryItem.prev != null)
						btnHistoryPrev.setEnabled(true);
					else
						btnHistoryPrev.setEnabled(false);					
				}
			}
		});
		btnHistoryPrev.setFont(new Font("Tahoma", Font.PLAIN, 11));
		btnHistoryPrev.setBounds(122, 58, 41, 23);
		frame.getContentPane().add(btnHistoryPrev);
		
		btnHistoryNext = new JButton("");
		btnHistoryNext.setEnabled(false);
		btnHistoryNext.setToolTipText("مورد بعدی در تاریخچه");
		btnHistoryNext.setIcon(new ImageIcon(KnowledgeExplorer.class.getResource("/javax/swing/plaf/metal/icons/sortUp.png")));
		btnHistoryNext.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) 
			{
				if (currentHistoryItem != null && currentHistoryItem.next != null)
				{
					currentHistoryItem = currentHistoryItem.next;
					
					textWord.setText(currentHistoryItem.word);
					comboPos.setSelectedIndex(currentHistoryItem.indexPos);
					comboCat.setSelectedIndex(currentHistoryItem.indexCat);
					updateModel(currentHistoryItem.modelSenses);
					listSenses.setSelectedIndex(currentHistoryItem.indexSenses);
					
					showSelectedConceptInfo(false);
					
					btnHistoryPrev.setEnabled(true);
					
					if (currentHistoryItem.next != null)
						btnHistoryNext.setEnabled(true);
					else
						btnHistoryNext.setEnabled(false);
				}
			}
		});
		btnHistoryNext.setFont(new Font("Tahoma", Font.PLAIN, 11));
		btnHistoryNext.setBounds(122, 28, 41, 23);
		frame.getContentPane().add(btnHistoryNext);
		
		frame.getRootPane().setDefaultButton(btnExactSearch);
		
		comboCat = new JComboBox();
		comboCat.setModel(new DefaultComboBoxModel(new String[] {"همه انواع مفهوم", "فقط معانی (Sense)"}));
		comboCat.setBounds(738, 62, 112, 20);
		frame.getContentPane().add(comboCat);
		
		JLabel lblNewLabel = new JLabel("بازیابی شده:");
		lblNewLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		lblNewLabel.setBounds(891, 448, 80, 16);
		frame.getContentPane().add(lblNewLabel);
		
		labelRetrieved = new JLabel("0");
		labelRetrieved.setHorizontalAlignment(SwingConstants.LEFT);
		labelRetrieved.setBounds(738, 450, 53, 16);
		frame.getContentPane().add(labelRetrieved);
		
		JButton button_3 = new JButton("");
		button_3.setToolTipText("بازنمایی گرافیکی");
		button_3.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) 
			{
				visualize();
			}
		});
		button_3.setIcon(new ImageIcon(KnowledgeExplorer.class.getResource("/com/sun/java/swing/plaf/windows/icons/ListView.gif")));
		button_3.setBounds(22, 448, 29, 23);
		frame.getContentPane().add(button_3);
		
		JLabel label_8 = new JLabel("پرسش و پاسخ:");
		label_8.setEnabled(false);
		label_8.setHorizontalAlignment(SwingConstants.RIGHT);
		label_8.setFont(new Font("Tahoma", Font.BOLD, 12));
		label_8.setBounds(866, 495, 97, 23);
		frame.getContentPane().add(label_8);
		
		comboPov = new JComboBox();
		comboPov.setEnabled(false);
		comboPov.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) 
			{
				populateHprVaribales();
			}
		});
		comboPov.setEditable(true);
		comboPov.setFont(new Font("Tahoma", Font.PLAIN, 12));
		comboPov.setMaximumRowCount(30);
		comboPov.setBounds(440, 573, 525, 25);
		comboPov.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
		
		frame.getContentPane().add(comboPov);
		
		JLabel label_4 = new JLabel("همبافت:");
		label_4.setEnabled(false);
		label_4.setHorizontalAlignment(SwingConstants.RIGHT);
		label_4.setFont(new Font("Tahoma", Font.PLAIN, 12));
		label_4.setBounds(513, 524, 46, 14);
		frame.getContentPane().add(label_4);
		
		textContext = new JTextField();
		textContext.setHorizontalAlignment(SwingConstants.RIGHT);
		textContext.setFont(new Font("Tahoma", Font.PLAIN, 12));
		textContext.setColumns(10);
		textContext.setBounds(440, 542, 120, 25);
		frame.getContentPane().add(textContext);
		
		JButton button_4 = new JButton("پاسخگویی");
		button_4.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) 
			{
				answerPov();
			}
		});
		button_4.setFont(new Font("Tahoma", Font.PLAIN, 12));
		button_4.setBounds(311, 542, 121, 27);
		frame.getContentPane().add(button_4);
		
		textMiniSearch = new JTextField();
		textMiniSearch.setBackground(SystemColor.info);
		textMiniSearch.setHorizontalAlignment(SwingConstants.RIGHT);
		textMiniSearch.setFont(new Font("Tahoma", Font.PLAIN, 12));
		textMiniSearch.setColumns(10);
		textMiniSearch.setBounds(624, 446, 90, 20);
		frame.getContentPane().add(textMiniSearch);
		
		JButton btnsj = new JButton("جستجو");
		btnsj.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) 
			{
				String search = textMiniSearch.getText().toLowerCase();
				
				if (search.equals(""))
				{
					message("لطفا حداقل یک حرف وارد نمایید تا در جعبه بالا جستجو شود!ـ");
					return;
				}
				
				DefaultHighlighter.DefaultHighlightPainter highlightPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.CYAN);
				
				String strTemp = textArea.getText().toLowerCase();
				
				int pos = strTemp.indexOf(search);
				
				if (pos != -1)
					textArea.setCaretPosition(pos);
				
				while (pos != -1)
				{		
					try 
					{
						textArea.getHighlighter().addHighlight(pos, pos + search.length(), highlightPainter);
					} 
					catch (BadLocationException e) 
					{
						e.printStackTrace();
					}
					
					pos = strTemp.indexOf(search, pos + 1);
				}				
			}
		});
		btnsj.setToolTipText("");
		btnsj.setFont(new Font("Tahoma", Font.PLAIN, 10));
		btnsj.setBounds(557, 445, 65, 23);
		frame.getContentPane().add(btnsj);
		
		JButton button_7 = new JButton("پاک");
		button_7.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) 
			{
				textArea.updateUI();
			}
		});
		button_7.setToolTipText("");
		button_7.setFont(new Font("Tahoma", Font.PLAIN, 10));
		button_7.setBounds(492, 445, 65, 23);
		frame.getContentPane().add(button_7);
	}
		
	private void print(String text)
	{
		textArea.append("\r\n" + text);
		
		textArea.setCaretPosition(0);
		textArea.getCaret().setVisible(true);
	}
	
	private void showSelectedConceptInfo(boolean recordHistory)
	{
		int index = listSenses.getSelectedIndex();
		
		if (index == -1)
			return;
		
		textArea.setText("");
		
		Node node = (Node)listSenses.getModel().getElementAt(index);
		
		StringBuilder text = new StringBuilder();
		String senseDump = "";
		
		if (node.isDynamic()) // separator node
		{
			return;
		}		
		else if (node.getLexicalType() == LexicalType.SENSE)
		{
			senseDump = _kb.getConceptDump(node, 50); // the sense
			
			ArrayList<PlausibleAnswer> synsets = node.findTargetNodes(KnowledgeBase.HPR_SYN);
			
			if (synsets.size() > 0)
				node = synsets.get(0).answer;
		}
		
		text.append(_kb.getConceptDump(node, 50));
		
		text.append("\r\n---------- بازنمایی متنی دانش ----------\r\n");
		
		text.append(node.getNodeKnowledgeHumanReadable(50));
		
		if (!senseDump.isEmpty())
		{
			text.append("\r\n\r\n-------------- رابطه های خود معنی ----------------\r\n" + senseDump);
		}
		
		// trimming lengthy lines
		
		StringBuilder out = new StringBuilder();
		
		String[] lines = text.toString().split("\r\n");
		
		for (String line: lines)
		{
			if (line.length() > 200)
				out.append(line.substring(0, 200) + "...\r\n");
			else
				out.append(line + "\r\n");
		}
		
		print(out.toString());
		
		textArea.setCaretPosition(0);
		
		//---
		
		if (recordHistory)
		{
			HistoryItem historyItem = new HistoryItem();
			
			historyItem.indexPos = comboPos.getSelectedIndex();
			historyItem.indexCat = comboCat.getSelectedIndex();
			historyItem.indexSenses = index;
			historyItem.modelSenses = modelList;
			historyItem.word = textWord.getText();
			historyItem.text = textArea.getText();				
			
			historyItem.prev = currentHistoryItem;
			historyItem.next = null;
			
			if (currentHistoryItem != null)
				currentHistoryItem.next = historyItem;
			
			currentHistoryItem = historyItem;				
			
			if (currentHistoryItem.prev != null)
				btnHistoryPrev.setEnabled(true);
		}
	}
	
	private void message(String msg)
	{
		JOptionPane.showMessageDialog(null, msg, "پیغام", JOptionPane.PLAIN_MESSAGE);
	}
	
	private boolean checkKbInit()
	{
		if (!isKbInitialized)
		{
			print("");
			print("لطفا یک پایگاه دانش با استفاده از منوی مربوطه حافظه بار نمایید سپس اقدام فرمایید.");
			
			return false;
		}
		
		if (textWord.getText().length() < 2)
		{
			message("جهت جستجو باید حداقل دو حرف وارد شود.");
			textWord.requestFocusInWindow();
			return false;
		}
		
		return true;
	}
	
	private static void addPopup(Component component, final JPopupMenu popup) {
		component.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger()) {
					showMenu(e);
				}
			}
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger()) {
					showMenu(e);
				}
			}
			private void showMenu(MouseEvent e) {
				popup.show(e.getComponent(), e.getX(), e.getY());
			}
		});
	}
	
	private void searchSensesFromSubstr(final StringMatch matchType)
	{
		if (!checkKbInit())
			return;			
		
		textArea.setText("");
		modelList = new DefaultListModel<Node>();
		
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				POS pos = Common.convertIntegerToPos(comboPos.getSelectedIndex());
				LexicalType cat = Common.getCategoryFromInteger(comboPos.getSelectedIndex());
				
				String word = textWord.getText().trim();
				
				Vector<Node> senses = _kb.getConceptFromSubstr(word, pos, matchType, cat);
				
				for (int i = 0; i < senses.size(); i++)
					modelList.addElement(senses.get(i));						
				
				//----------------- checking against normalized and lemmatized versions ------------------
				
				String normalized = word;
				String lemmatized = word;
				
				if (modelList.size() == 0)
				{
					textArea.setText("موردی یافت نشد");
					
					updateModel(modelList);
					
					return;
				}
				
				textArea.setText("");
				
				updateModel(modelList);
				
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						scrollSenses.getHorizontalScrollBar().setValue(0);
					}
				});
			}
		});
	}
	
	private void updateModel(DefaultListModel<Node> model)
	{
		if (model == null)
			return;
		
		listSenses.setModel(model);
		
		labelRetrieved.setText(new Integer(modelList.size()).toString());
	}
	
	private void extractHierarchy(Node node, int depth)
	{
		if (depth > 10)
			return;
		
		String out = "";
		
		for (int i = 0; i < depth; i++)
			out += "\t";
		
		out += /*depth + ". " + */ node.getName();
		
		print(out);
		
		ArrayList<PlausibleAnswer> hyponyms = node.findSourceNodes(KnowledgeBase.HPR_ISA);
		
		for (PlausibleAnswer hyponym: hyponyms)
		{
			extractHierarchy(hyponym.answer, depth + 1);
		}
	}
	
	private void findSynonymAntonym(MethodType type)
	{
		Node separator = new Node("------------------------");
		separator.setConceptType(ConceptType.CONCEPT_OTHER);
		separator.setDynamic();
		
		//-------------------------------------------------------------
		
		POS pos = Common.convertIntegerToPos(comboPos.getSelectedIndex());
		
		ArrayList<PlausibleAnswer[]> answersets = null;
		
		if (type == MethodType.ANTONYM)
		{
			answersets = _re.findAntonyms(textWord.getText(), pos);
		}
		else if (type == MethodType.SYNONYM)
		{
			answersets = _re.findSynonyms(textWord.getText(), pos);
		}
		else
		{
			MyError.exit("Invalid method conceptType!");
		}
			
		if (answersets != null && answersets.size() > 0)
		{
			for (PlausibleAnswer[] answerset: answersets)
			{
				if (answerset != null && answerset.length > 0)
				{
					for (int i = 0; i < answerset.length; i++)
					{
						modelList.addElement(answerset[i].answer);
					}
				}
				
				modelList.addElement(separator);
			}
		}
				
		if (modelList.size() == 0)
		{
			textArea.setText("موردی یافت نشد");
			
			updateModel(modelList);
			
			return;
		}
		
		textArea.setText("");
		
		updateModel(modelList);
	}
	
	private void loadPreprocessor(PreprocessorType preprocessType)
	{
		/*
		if ((preprocessType == PreprocessorType.NORMILIZATION || preprocessType == PreprocessorType.POSTAGGING) && _normalizer == null)
		{
			print("بار کردن یکسان ساز ...");
			
			_normalizer = new PersianEditor();
			
			print("انجام شد");
			print("");
		}
		
		if (preprocessType == PreprocessorType.LEMMATIZATION && _lemmatizer == null)
		{
			print("بار کردن لم یاب ...");
			
			_lemmatizer = new MorphologicalAnalyzer();
			//_lemmatizer.loadLemmatizer();
			
			print("انجام شد");
			print("");
		}
		
		if (preprocessType == PreprocessorType.POSTAGGING && _postagger == null)
		{
			print("بار کردن برچسب گذار صرفی ...");
			
			_postagger = new PersianPOSTagger();
			_postagger.load();
			
			print("انجام شد");
			print("");
		}
		*/		
	}
	
	
	
	private void preprocessConcepts(PreprocessorType preprocessType)
	{		
		/*
		if (!isKbInitialized)
		{
			print("");
			print("لطفا یک پایگاه دانش با استفاده از منوی مربوطه حافظه بار نمایید سپس اقدام فرمایید.");
			
			return;
		}
		
		textArea.setText("");
		
		loadPreprocessor(preprocessType);
		
		//--------------------------------------------------------------------------
		
		Entry<String, Node> entry;
		Node node;
		
		String preprocessed, preprocessed_standard, name, name_standard;
		
		print("ردیف\tاصلی\t\t\tپردازش شده");
		print("-----\t----------\t\t\t---------------");
		
		int j = 0;
		
		while (iteratorConcept.hasNext()) 
		{                        
			entry = (Entry<String, Node>)iteratorConcept.next();
			
			name = entry.getKey();
			node = entry.getValue();
			
			if (node.source == SourceType.WORDNET) // not for English resources
				continue;
			
			if (node.lexicalType == LexicalType.SYNSET) // not for synset nodes
				continue;
			
			if (node.conceptType == ConceptType.CONCEPT_EXAMPLE || node.conceptType == ConceptType.CONCEPT_GLOSS || node.conceptType == ConceptType.STATEMENT)
				continue;
			
			if (name.contains("_n-") || Common.isEnglish(name)) // ignoring reference nodes
				continue;
			
			name = Common.removeSenseInfo(name);
			preprocessed = "";
			
			switch (preprocessType)
			{
				case NORMILIZATION:; preprocessed = _normalizer.Virastkar(_normalizer.tokenizer(_normalizer.normalizer(name))); break;
				case LEMMATIZATION:; preprocessed = _lemmatizer.getLemma(name); break;
				case POSTAGGING:; preprocessed = _postagger.tagSentence(_normalizer.tokenizer(_normalizer.normalizer(name)).trim()); break;
			}
			
			preprocessed = preprocessed.trim();
			
			name_standard = name;
			preprocessed_standard = preprocessed;
			
			if (checkShowObvoius.isSelected())
			{
				name_standard 			= getCanonical(name_standard, preprocessType);				
				preprocessed_standard 	= getCanonical(preprocessed_standard, preprocessType);
			}
			
			if (!name_standard.equals(preprocessed_standard))
			{
				print(counterConcept + "\t" + name + "\t\t\t" + preprocessed);
				
				j++;
				counterConcept++;
				
				if (j == 100)
					break;
			}
		}
		*/
	}
	
	private void visualize()
	{
		int index = listSenses.getSelectedIndex();
		
		if (index == -1)
			return;
		
		Node node = (Node)listSenses.getModel().getElementAt(index);
		
		if (node == null)
			return;
		
		visualizeNode(node, currentGraphLayout);
	}
	
	/*
	private Class<? extends Layout>[] getCombos()
    {
        List<Class<? extends Layout>> layouts = new ArrayList<Class<? extends Layout>>();
        layouts.add(KKLayout.class);
        layouts.add(FRLayout.class);
        layouts.add(CircleLayout.class);
        layouts.add(SpringLayout.class);
        layouts.add(SpringLayout2.class);
        layouts.add(ISOMLayout.class);
        return layouts.toArray(new Class[0]);
    }
    */
	
	private void visualizeNode(Node node, GraphLayouts graphLayout)
	{
		VisualizationViewer<Node, String> vv = getGraphFromNode(node, graphLayout);
		
		frame.setTitle("نمای گرافیکی در طرح " + graphLayout.toString());
		
		currentVisualNode = node;
		
		/*
		final JComboBox jcb = new JComboBox(getCombos());
        // use a renderer to shorten the layout name presentation
        jcb.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                String valueString = value.toString();
                valueString = valueString.substring(valueString.lastIndexOf('.')+1);
                return super.getListCellRendererComponent(list, valueString, index, isSelected,
                        cellHasFocus);
            }
        });
        
        vv.add(jcb);
		*/
		
		frameGraph.getContentPane().removeAll();
		frameGraph.getContentPane().add(vv);
		frameGraph.pack();		
		frameGraph.revalidate();
		frameGraph.repaint();		
		frameGraph.setVisible(true);
		
		//---
		
		textWord.setText(currentVisualNode.getName());
		//btnExactSearch.doClick();
	}
	
	private VisualizationViewer<Node, String> getGraphFromNode(Node node, GraphLayouts graphLayout)
	{
		DirectedSparseGraph<Node, String> graph = node.getJungGraph();
		
		Dimension dimention = Toolkit.getDefaultToolkit().getScreenSize();
		
		dimention.setSize(dimention.width - 15, dimention.height - 80);

		Layout layout = null;
		
		switch (graphLayout)
		{
			case KK		: layout = new KKLayout(graph); break;
			case CIRCLE	: layout = new CircleLayout(graph); break;
			case FR		: layout = new FRLayout(graph); break;
			case ISO	: layout = new ISOMLayout(graph); break;
			case SPRING1: layout = new SpringLayout(graph); break;
			case SPRING2: layout = new SpringLayout2(graph); break;
		}
		
		VisualizationViewer<Node, String> vv = new VisualizationViewer<Node, String>(layout, dimention);
		
		vv.setBackground(Color.white);
	
		vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
		
		vv.getRenderer().getVertexLabelRenderer().setPosition(Renderer.VertexLabel.Position.AUTO);
		
		VertexFontTransformer<Node> vff;
		vff = new VertexFontTransformer<Node>();
		vv.getRenderContext().setVertexFontTransformer(vff);
		
        vv.getRenderContext().setEdgeDrawPaintTransformer(new ConstantTransformer(Color.lightGray));
        vv.getRenderContext().setVertexFillPaintTransformer(new FillColorTransformer<Node>(vv.getPickedVertexState()));

		DefaultModalGraphMouse gm = new DefaultModalGraphMouse();
		gm.setMode(ModalGraphMouse.Mode.PICKING);
        gm.add(new MyPopupGraphMousePlugin());
		vv.setGraphMouse(gm);
		
		vv.setVertexToolTipTransformer(new NodeTooltip<Node>());
		
		VertexShapeSizeAspect<Node, String> vssa = new VertexShapeSizeAspect<Node, String>(graph);
		vv.getRenderContext().setVertexShapeTransformer(vssa);
		
		return vv;
	}
	
	private final static class VertexShapeSizeAspect<V,E> extends AbstractVertexShapeTransformer <V> implements Transformer<V, Shape>  
	{
        protected Graph<V,E> graph;
        
        public VertexShapeSizeAspect(Graph<V,E> graphIn)
        {
        	this.graph = graphIn;
            
        	setSizeTransformer(new Transformer<V,Integer>() 
        	{
				public Integer transform(V v) 
				{
					Node node = (Node)v;
			        	
		        	if (node.getConceptType() == ConceptType.STATEMENT)
		            {
		        		return 17;
		            }
		        	else
		        		return 20;
				}});            
        }
          
        public Shape transform(V v)
        {
            Node node = (Node)v;
        	
        	if (node.getConceptType() == ConceptType.STATEMENT)
            {
        		//return factory.getRegularStar(v, graph.degree(v));
        		
        		//int sides = Math.max(graph.degree(v), 3);
                return factory.getRegularPolygon(v, 4);                
        		
            }
            else
                return factory.getEllipse(v);
        }
    }
	
	private final static class VertexFontTransformer<V>	implements Transformer<V,Font>
	{
	    public Font transform(V v)
	    {
	    	return new Font("Tahoma", Font.PLAIN, 12);
	    }
	}
	
	protected class MyPopupGraphMousePlugin extends AbstractPopupGraphMousePlugin implements MouseListener 
	{
	    public MyPopupGraphMousePlugin() {
	        this(MouseEvent.BUTTON3_MASK);
	    }
	    public MyPopupGraphMousePlugin(int modifiers) {
	        super(modifiers);
	    }

	    /**
	     * If this event is over a node, pop up a menu to
	     * allow the user to center view to the node
	     *
	     * @param e
	     */
	    protected void handlePopup(MouseEvent e) 
	    {
	        final VisualizationViewer<Node,String> vv = (VisualizationViewer<Node,String>)e.getSource();
	        final Point p = e.getPoint(); 

	        GraphElementAccessor<Node,String> pickSupport = vv.getPickSupport();
	        if(pickSupport != null) 
	        {
	            final Node station = pickSupport.getVertex(vv.getGraphLayout(), p.getX(), p.getY());
	            if (station != null) 
	            {
	            	currentGraphLayout = GraphLayouts.KK;
	            	
	            	visualizeNode(station, currentGraphLayout);
	            }
	            else
	            {
	            	currentGraphLayout = currentGraphLayout.next();
	            	
	            	visualizeNode(currentVisualNode, currentGraphLayout);
	            }
	        }
	    }
	}
	
	private final class FillColorTransformer<Node> implements Transformer<Node,Paint>
    {
		PickedInfo<Node> _nodePicked;
		
		public FillColorTransformer(PickedInfo<Node> pi)
		{
			_nodePicked = pi;
		}
				
		public Paint transform(Node v)
		{
			ir.ac.itrc.qqa.semantic.kb.Node node = (ir.ac.itrc.qqa.semantic.kb.Node)v; 
			
			if (_nodePicked.isPicked(v))
			{
				return Color.YELLOW;
			}
			else if (currentVisualNode != null && currentVisualNode == v)
			{
				return Color.RED;
			}
			else if (node.hasReference())
			{
				return Color.CYAN;
			}			
			
			return Color.LIGHT_GRAY;
		}
    }
	
	public class NodeTooltip<E>	implements Transformer<Node,String> 
	{	 
		public String transform(Node node) 
		{
			String tooltip = Common.breakLine(node.getName(), 60);
			
			tooltip += "<br><br>منبع: " + node.getSourceType().getFarsiName();
			
			return "<html>" + tooltip + "</html>";
		}
	}
	
	private class Pov
	{
		public String question;
		public String object;
		public String property;
		public String value;
		public String context;
		
		@Override
		public String toString()
		{
			return question;
		}
	}
	

	private void populateHprVaribales()
	{
		Pov pov = (Pov)comboPov.getItemAt(comboPov.getSelectedIndex());
		
		if (pov == null)
			return;
		
		textDescriptor.setText(pov.property);
		textArgument.setText(pov.object);
		textReferent.setText(pov.value);	
		textContext.setText(pov.context);
	}
	
	private void showKbStats(boolean clearScreen)
	{
		if (clearScreen)
			textArea.setText("");
		else
			print("");
		
		print("------------------------------------------");
		print("آمار دانش بار شده در حافظه:");
		
		String output = _kb.getStatistics();
		
		print("");
		print(output);
		
		print(":از منابع");
		print(_kb.getLoadedKbs().toString());
	}
	
	private void answerPov()
	{
		int maxDepth = Integer.parseInt(spinnerMaxReasonongDepth.getValue().toString());
		int maxAnswers = Integer.parseInt(spinnerMaxReasonongAnswers.getValue().toString());
		
		_re.setMaxReasoningDepth(maxDepth);
		_re.setMaximumAnswers(maxAnswers);
		
		if (textDescriptor.getText().isEmpty())
		{
			message("لطفا رابطه را وارد نمایید");
			textDescriptor.requestFocusInWindow();
			return;
		}
		else if (textArgument.getText().isEmpty() && textReferent.getText().isEmpty())
		{
			message("یا مبداء و یا مقصد رابطه باید ذکر گردد");
			textArgument.requestFocusInWindow();
			return;
		}		
		
		if (comboPov.getModel().getSize() > 0 && comboPov.getSelectedItem() != null)
			_lastNaturalQuestion = (String)comboPov.getSelectedItem().toString();
		
		_lastPlausibleQuestion = null;
		_lastAnswers = null;
		
		textArea.setText("");
		
		ArrayList<PlausibleAnswer> pas = _re.answerPovQuestion(textArgument.getText(), textDescriptor.getText(), textReferent.getText());
		
		_lastAnswers = pas;
		
		if (pas.size() > 0)
		{
			PlausibleQuestion pq = pas.get(0).question;
			
			if (pq != null)
			{
				print(pq.toString());
				print("");
			}
			
			_lastPlausibleQuestion = pq;
		}
		
		int i = 0;
		for (PlausibleAnswer pa: pas)
		{
			i++;
			
			print("");
			print("-------------------------------");
			print("");
			
			print(i + ": " + pa.toString());
			print(pa.certaintyToString());
			
			_lastPlausibleQuestion = pa.question;
		}
		
		print("");
		print("~~~~~~~~~~~~~~~~~~~~~~~~~~");
		print("");
		
		print("تعداد کل استنباط های انجام شده: " + _re.totalCalls);
		print("زمان کل استدلال: " + _re.reasoningTime/1000 + " ثانیه");
	}
	
	private void showAnswerJustifications()
	{
		if (_lastAnswers == null || _lastAnswers.size() == 0)
			return;
		
		BufferedWriter MyStream = null;
		
		try {
			MyStream = new BufferedWriter(new FileWriter("Justifications.html"));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		int counter = 0;
		int counterJust;
		int answerCount = 0;
		ArrayList<String> justifications;
		String sign;

		String Text = "<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'><title>توجیه پاسخ ها</title></head><body dir='rtl' style='line-height:16pt;'><font face=tahoma size=2>";

		Text += "<p><font size=2 color=gray><b>پرسش زبان طبیعی: </font><font size=2 color=black>" + _lastNaturalQuestion + "</font></b>";
		Text += "<p><font size=2 color=gray><b>پرسش مقبول: </font><font size=2 color=black>" + _lastPlausibleQuestion + "</font></b>";
		Text += "<p><font size=2 color=gray><b>تعداد استنباط های پایه فراخوانی شده: </font><font size=2 color=black>" + _re.totalCalls + "</font></b>";
		Text += "<p><font size=2 color=gray><b>زمان کل استدلال: </font><font size=2 color=black>" + _re.reasoningTime/1000 + " ثانیه</font></b>";
		Text += "<HR>";
		
		
		for (PlausibleAnswer pa: _lastAnswers)
		{
			answerCount++;

			Text += "<font size=2 color=gray><b>پاسخ شماره " + answerCount + ": </b></font>";

			Text += "<font size=2 color=navy><b>" + pa.toString() + "</b></font><p>";
			Text += "<font size=2 color=gray><b>اطمینان: </b></font>";
			Text += "<font size=2 color=blue><b>" + pa.certaintyToString(false) + "</b></font><p>";

			Text += "<font size=2 color=gray><b>مشروط به:<p></font></b>";

			counter = 0;
			for (String Condition: pa.getConditions())
			{
				counter++;
				Text += "<font size=2 color=gray>" + counter + ". </font>" + Condition + "<br>";
			}
			if (counter == 0)
			{
				Text += "<font size=2 color=black>بدون شرط</font>";
			}

			Text += "<p><hr>";


			counterJust = 0;
			justifications = pa.getTaggedNaiveJustifications(false);
			for (String justification: justifications)
			{
				counterJust++;

				Text += "<font size=2 color=gray><b>توجیه شماره " + counterJust + ":</font></b><p>";
				Text += justification;
				
				if (counterJust != justifications.size())
				{
					Text += "<hr width=30% align=right>";
					Text += "<p>";
				}
			}
			
			Text += "<hr>";
		}

		try 
		{
			MyStream.write(Text);
		} 
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try 
		{
			MyStream.close();
		} 
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		File file = new File("Justifications.html");

		try 
		{
			Desktop.getDesktop().browse(file.toURI());
		} 
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}