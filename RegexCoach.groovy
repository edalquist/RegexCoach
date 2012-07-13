// Groovy Regex Coach - Copyright 2007 Jeremy Rayner
// inspired by http://weitz.de/regex-coach/
import java.awt.*
import java.awt.event.*
import java.util.regex.*
import javax.swing.*
import javax.swing.event.*
import javax.swing.text.DefaultHighlighter
import groovy.swing.SwingBuilder
import java.util.concurrent.atomic.*

// define the view
def swing = new SwingBuilder()
def gui = swing.frame(title:"The Groovy Regex Coach", location:[20,40], size:[600,600], defaultCloseOperation:WindowConstants.EXIT_ON_CLOSE) {
    panel(layout:new BorderLayout()) {
        splitPane(orientation:JSplitPane.VERTICAL_SPLIT, dividerLocation:450) {
            splitPane(orientation:JSplitPane.VERTICAL_SPLIT, dividerLocation:200) {
                panel(layout:new BorderLayout()) {
                    label(constraints:BorderLayout.NORTH, text:"Regular expression:")
                    scrollPane(constraints:BorderLayout.CENTER) {textPane(id:"regexPane", text:"oo (the) bar")}
                    panel(constraints:BorderLayout.SOUTH, layout:new BorderLayout()) {
                        label(constraints:BorderLayout.WEST, id:"regexStatus", text:" ")
                        panel(constraints:BorderLayout.EAST) {
                            checkBox(id:"regexOptCaseInsensitive", text:"i", toolTipText:"Case Insensitive")    //CASE_INSENSITIVE 
                            checkBox(id:"regexOptMultiline", text:"m", toolTipText:"Multline")  //MULTILINE 
                            checkBox(id:"regexOptDotAll", text:"s", toolTipText:"Dot All") //DOTALL
                            checkBox(id:"regexOptComments", text:"x", toolTipText:"Comments") //COMMENTS
                            //TODO CANON_EQ, LITERAL, UNICODE_CASE, UNIX_LINES
                        }
                    }
                }
                panel(layout:new BorderLayout()) {
                    label(constraints:BorderLayout.NORTH, text:"Target string:")
                    scrollPane(constraints:BorderLayout.CENTER) {textPane(id:"targetPane", text:"foo the bar oo the bar")}
                    panel(constraints:BorderLayout.SOUTH, layout:new BorderLayout()) {
                        label(constraints:BorderLayout.NORTH, id:"targetStatus", text:" ")
                    }
                }
            }
            panel(layout:new BorderLayout()) {
                highlightGroup = buttonGroup();
                tableLayout {
                    tr {
                        td(colspan:3) {
                            label("Highlight")
                        }
                    }
                    tr {
                        td {
                            radioButton(id:"highlightSelection", text:"selection", buttonGroup:highlightGroup, enabled:false)
                        }
                        td {
                            radioButton(id:"highlightNone", text:"nothing", buttonGroup:highlightGroup)
                        }
                        td {
                            panel(layout:new FlowLayout()) {
                                radioButton(id:"highlightGroup", text:"Group", buttonGroup:highlightGroup, selected:true)
                                spinner(id:"highlightGroupNumber", model:spinnerNumberModel(value:0, minimum:0, maximum:1, stepSize:1))
                                label(text:"/")
                                label(id:"groupCount", text:"0")
                            }
                        }
                    }
                    tr {
                        td {
                            label(id:"highlightSelectionLabel", text:"")
                        }
                        td {
                            label(id:"highlightNothingLabel", text:"")
                        }
                        td {
                            label(id:"highlightGroupLabel", text:"")
                        }
                    }
                    tr {
                        td(colspan:3) {
                            panel(layout:new BorderLayout()) {
                                panel(layout:new FlowLayout(), constraints:BorderLayout.WEST) {
                                    label(text:"Match")
                                    spinner(id:"matchNumber", model:spinnerNumberModel(value:1, minimum:1, maximum:1, stepSize:1))
                                    label(text:"/")
                                    label(id:"matchCount", text:"0")
                                }
                                panel(layout:new FlowLayout(), constraints:BorderLayout.CENTER) {
                                    label(text:"String Start")
                                    spinner(id:"startOfString", model:spinnerNumberModel(value:0, minimum:0, maximum:0, stepSize:1))
                                }
                                panel(layout:new FlowLayout(), constraints:BorderLayout.EAST) {
                                    label(text:"String End")
                                    spinner(id:"endOfString", model:spinnerNumberModel(value:0, minimum:0, maximum:0, stepSize:1))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
def highlighter = new RegexHighlighter(swing:swing)

//Text area mod listeners
swing.regexPane.addKeyListener(highlighter)
swing.regexPane.addCaretListener(highlighter)
swing.targetPane.addKeyListener(highlighter)

//Regex Options listeners
swing.regexOptCaseInsensitive.addActionListener(highlighter)
swing.regexOptMultiline.addActionListener(highlighter)
swing.regexOptDotAll.addActionListener(highlighter)
swing.regexOptComments.addActionListener(highlighter)

//Match change listeners
swing.matchNumber.addChangeListener(highlighter)
swing.startOfString.addChangeListener(highlighter)
swing.endOfString.addChangeListener(highlighter)

//Selection listeners
swing.highlightSelection.addActionListener(highlighter)
swing.highlightNone.addActionListener(highlighter)
swing.highlightGroup.addActionListener(highlighter)
swing.highlightGroupNumber.addChangeListener(highlighter)




gui.show()
highlighter.doHighlights()

class RegexHighlighter extends KeyAdapter implements ActionListener, ChangeListener, CaretListener {
    final AtomicBoolean inHighlights = new AtomicBoolean(false); //prevents concurrent ui updates
    final AtomicInteger refreshCounter = new AtomicInteger();
    def swing // reference to the view
    def prevTargetLength = 0
    def orange = new DefaultHighlighter.DefaultHighlightPainter(Color.ORANGE)
    def yellow = new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW)
    def red = new DefaultHighlighter.DefaultHighlightPainter(Color.RED)
    def gray = new DefaultHighlighter.DefaultHighlightPainter(Color.GRAY)

    // react to user actions
    public void actionPerformed(ActionEvent event) {
        doHighlights()
    }
    public void keyReleased(KeyEvent event) {
        doHighlights()
    }
    public void stateChanged(ChangeEvent e) {
        doHighlights()
    }
    public void caretUpdate(CaretEvent e) {
        //TODO uncomment when state based listener exists doHighlights()
    }

    private resetView() {
        //TODO need to account for caret position
    
        //Capture user selection
        def regexSelectStart = swing.regexPane.selectionStart;
        def regexSelectEnd = swing.regexPane.selectionEnd;
        def targetSelectStart = swing.targetPane.selectionStart;
        def targetSelectEnd = swing.targetPane.selectionEnd;
        
        //Remove all highlights
        swing.regexPane.highlighter.removeAllHighlights()
        swing.targetPane.highlighter.removeAllHighlights()
        swing.regexStatus.text = " "
        swing.targetStatus.text = " "
        
        //Re-enable the user's selection
        if (regexSelectStart < regexSelectEnd) {
            swing.regexPane.select(regexSelectStart, regexSelectEnd);
            swing.regexPane.caret.setSelectionVisible(true);
        }
        if (targetSelectStart < targetSelectEnd) {
            swing.targetPane.select(targetSelectStart, targetSelectEnd);
            swing.targetPane.caret.setSelectionVisible(true);
        }
    }
    
    // the main regex logic
    public doHighlights() {
        refreshCounter.incrementAndGet();
        if (!inHighlights.compareAndSet(false, true)) {
            return;
        }
        try {
            while(refreshCounter.get() > 0) {
                refreshCounter.getAndDecrement()
                
                resetView()
                // note: get the text from the underlying document,
                // otherwise carriage return/line feeds different when using the JTextPane text
                def regex = swing.regexPane.document.getText(0,swing.regexPane.document.length)
                def target = swing.targetPane.document.getText(0,swing.targetPane.document.length)
                
                // Generate regex with capture group inserted at selection
                def regexSelectStart = swing.regexPane.selectionStart;
                def regexSelectEnd = swing.regexPane.selectionEnd;
                def selectionRegex = null;
                if (regexSelectStart < regexSelectEnd) {
                    selectionRegex = regex.substring(regexSelectStart, regexSelectEnd);
                }

                if (prevTargetLength != target.length()) {
                    //Update endOfString spinner
                    swing.endOfString.model.maximum = target.length();
                    if (prevTargetLength <= swing.endOfString.value) {                
                        swing.endOfString.value = target.length();
                    }
                    
                    //Update startOfString spinner
                    if (swing.endOfString.value > 0) {
                        swing.startOfString.model.maximum = swing.endOfString.value - 1;
                    }
                    else {
                        swing.startOfString.model.maximum = 0;
                    }
                    if (swing.startOfString.value > swing.startOfString.model.maximum) {
                        swing.startOfString.value = swing.startOfString.model.maximum;
                    }
                    
                    prevTargetLength = target.length();
                }
                
                //Highlight the areas that are being trimmed and trim the target string
                swing.targetPane.highlighter.addHighlight(0, swing.startOfString.value, gray);
                swing.targetPane.highlighter.addHighlight(swing.endOfString.value, target.length(), gray);
                target = target.substring(swing.startOfString.value, swing.endOfString.value);

                
                //Build regex flags
                int patternFlags = 0;
                if (swing.regexOptCaseInsensitive.selected) {
                    patternFlags |= Pattern.CASE_INSENSITIVE
                }
                if (swing.regexOptMultiline.selected) {
                    patternFlags |= Pattern.MULTILINE
                }
                if (swing.regexOptDotAll.selected) {
                    patternFlags |= Pattern.DOTALL
                }
                if (swing.regexOptComments.selected) {
                    patternFlags |= Pattern.COMMENTS
                }
                
                def pattern = Pattern.compile(regex, patternFlags);
                def matcher = pattern.matcher(target);
                
                //Count matches & update match spinner
                def matchCount = 0;
                while (matcher.find()) {
                    matchCount++;
                }
                matcher.reset();
                swing.matchNumber.model.maximum = matchCount;
                swing.matchCount.text = matchCount;
                if (swing.matchNumber.value > swing.matchNumber.model.maximum) {
                    swing.matchNumber.value = swing.matchNumber.model.maximum
                }
                
                //Find the appropriate match
                for (def match = 0; match < swing.matchNumber.value - 1; match++) {
                    matcher.find();
                }
                if (matcher.find()) {
                    //Do matching of the regex selection
                    def selectionMatcher = null;
                    def selectionMatched = false;
                    if (selectionRegex != null) {
                        try {
                            def selectionPattern = Pattern.compile(selectionRegex, patternFlags);
                            selectionMatcher = selectionPattern.matcher(matcher.group(0));
                            selectionMatched = selectionMatcher.find()
                        }
                        catch (Exception e) {
                        }
                    }
                    swing.highlightSelection.enabled = selectionMatched;
                    
                    //Update group count spinner
                    swing.highlightGroupNumber.model.maximum = matcher.groupCount();
                    swing.groupCount.text = matcher.groupCount();
                    if (swing.highlightGroupNumber.value > swing.highlightGroupNumber.model.maximum) {
                        swing.highlightGroupNumber.value = swing.highlightGroupNumber.model.maximum
                    }
                    
                    def startOffset = swing.startOfString.value;

                    //Highlight selection/group                    
                    if (swing.highlightSelection.selected && selectionMatched) {
                        swing.targetPane.highlighter.addHighlight(startOffset + matcher.start() + selectionMatcher.start(), startOffset +  + matcher.start() + selectionMatcher.end(), orange)
                    }
                    else if (swing.highlightGroup.selected) {
                        def groupNumber = swing.highlightGroupNumber.value;
                        swing.targetPane.highlighter.addHighlight(startOffset + matcher.start(groupNumber), startOffset + matcher.end(groupNumber), orange)
                    }

                    // highlight whole match
                    swing.targetPane.highlighter.addHighlight(startOffset + matcher.start(), startOffset + matcher.end(), yellow)
                    if (regex.length() != 0) {
                        swing.targetStatus.text = "Match #${swing.matchNumber.value} from ${matcher.start()} to ${matcher.end()}."
                    }
                }
                else {
                    swing.targetStatus.text = "No match."
                }
            }
        } catch (PatternSyntaxException e) {
            swing.regexPane.highlighter.addHighlight(e.index, e.index + 2, red)
            swing.regexStatus.text = e.description
        }
        finally {
            inHighlights.set(false);
        }
    }
}
