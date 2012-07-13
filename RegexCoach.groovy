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
        splitPane(orientation:JSplitPane.VERTICAL_SPLIT, dividerLocation:410) {
            splitPane(orientation:JSplitPane.VERTICAL_SPLIT, dividerLocation:200) {
                panel(layout:new BorderLayout()) {
                    label(constraints:BorderLayout.NORTH, text:"Regular expression:")
                    scrollPane(constraints:BorderLayout.CENTER) {textPane(id:"regexPane", text:"oo (the) bar")}
                    label(constraints:BorderLayout.SOUTH, id:"regexStatus", text:" ")
                }
                panel(layout:new BorderLayout()) {
                    label(constraints:BorderLayout.NORTH, text:"Target string:")
                    scrollPane(constraints:BorderLayout.CENTER) {textPane(id:"targetPane", text:"foo the bar oo the bar")}
                        label(constraints:BorderLayout.SOUTH, id:"targetStatus", text:" ")
                }
            }
            panel(layout:new BorderLayout()) {
                highlightGroup = buttonGroup();
                panel(constraints:BorderLayout.NORTH) {
                    tableLayout {
                        tr {
                            td {
                                checkBox(id:"regexOptCaseInsensitive", text:"Case Insensitive")    //CASE_INSENSITIVE 
                            }
                            td {
                                checkBox(id:"regexOptMultiline", text:"Multline")  //MULTILINE
                            }
                            td { 
                                checkBox(id:"regexOptDotAll", text:"Dot All") //DOTALL
                            }
                            td {
                                checkBox(id:"regexOptComments", text:"Comments") //COMMENTS
                            }
                        }
                        tr {
                            td {
                                checkBox(id:"regexOptCanonEq", text:"Canonical Eq") //CANON_EQ
                            }
                            td {
                                checkBox(id:"regexOptLiteral", text:"Literal") //LITERAL
                            }
                            td {
                                checkBox(id:"regexOptUnicodeCase", text:"Unicode-Aware Case") //UNICODE_CASE
                            }
                            td {
                                checkBox(id:"regexOptUnixLines", text:"Unix Lines") //UNIX_LINES
                            }
                        }
                    }
                }
                panel(constraints:BorderLayout.SOUTH) {
                    tableLayout {
                        tr {
                            td {
                                label("Highlight:")
                            }
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
                            }
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
                            td(colspan:4) {
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
swing.regexOptCanonEq.addActionListener(highlighter)
swing.regexOptLiteral.addActionListener(highlighter)
swing.regexOptUnicodeCase.addActionListener(highlighter)
swing.regexOptUnixLines.addActionListener(highlighter)

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

class UiState {
    final String regex;
    final String selectionRegex;
    final String target;
    final int regexSelectStart;
    final int regexSelectEnd;
    final int regexSelectCaret;
    final int targetSelectStart;
    final int targetSelectEnd;
    final int targetSelectCaret;
    final int patternFlags;
    final int targetStart;
    final int targetEnd;
    final int matchIndex;
    final boolean highlightSelection;
    final boolean highlightGroup;
    final int highlightGroupNumber;
    
    int matchCount = 0;
    int groupCount = 0;
    int matchStart = 0;
    int matchEnd = 0;
    int highlightStart = 0;
    int highlightEnd = 0;
    boolean selectionMatched = false;
    
    public UiState(def swing) {
        // note: get the text from the underlying document,
        // otherwise carriage return/line feeds different when using the JTextPane text
        regex = swing.regexPane.document.getText(0,swing.regexPane.document.length)
        target = swing.targetPane.document.getText(0,swing.targetPane.document.length)
        
        //Capture selected range for the regex
        regexSelectStart = swing.regexPane.selectionStart;
        regexSelectEnd = swing.regexPane.selectionEnd;
        regexSelectCaret = swing.regexPane.caret.dot;
        
        //Capture selected range for the target
        targetSelectStart = swing.targetPane.selectionStart;
        targetSelectEnd = swing.targetPane.selectionEnd;
        targetSelectCaret = swing.targetPane.caret.dot;
        
        // Generate regex for of just the selected portion
        def regexSelectStart = swing.regexPane.selectionStart;
        def regexSelectEnd = Math.min(swing.regexPane.selectionEnd, regex.length());
        if (regexSelectStart < regexSelectEnd) {
            selectionRegex = regex.substring(regexSelectStart, regexSelectEnd);
        }
        else {
            selectionRegex = null;
        }
        
        //Build regex flags
        int patternFlagsBuilder = 0;
        if (swing.regexOptCaseInsensitive.selected) {
            patternFlagsBuilder |= Pattern.CASE_INSENSITIVE
        }
        if (swing.regexOptMultiline.selected) {
            patternFlagsBuilder |= Pattern.MULTILINE
        }
        if (swing.regexOptDotAll.selected) {
            patternFlagsBuilder |= Pattern.DOTALL
        }
        if (swing.regexOptComments.selected) {
            patternFlagsBuilder |= Pattern.COMMENTS
        }
        if (swing.regexOptCanonEq.selected) {
            patternFlagsBuilder |= Pattern.CANON_EQ
        }
        if (swing.regexOptLiteral.selected) {
            patternFlagsBuilder |= Pattern.LITERAL
        }
        if (swing.regexOptUnicodeCase.selected) {
            patternFlagsBuilder |= Pattern.UNICODE_CASE
        }
        if (swing.regexOptUnixLines.selected) {
            patternFlagsBuilder |= Pattern.UNIX_LINES
        }
        patternFlags = patternFlagsBuilder;
        
        //Get trim start/end
        targetStart = Math.min(swing.startOfString.value, target.length());
        targetEnd = Math.min(swing.endOfString.value, target.length());
        
        //Get match index
        matchIndex = swing.matchNumber.value;
        
        //Get highlight info
        highlightSelection = swing.highlightSelection.selected;
        highlightGroup = swing.highlightGroup.selected;
        highlightGroupNumber = swing.highlightGroupNumber.value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (highlightGroup ? 1231 : 1237);
        result = prime * result + highlightGroupNumber;
        result = prime * result + (highlightSelection ? 1231 : 1237);
        result = prime * result + matchIndex;
        result = prime * result + patternFlags;
        result = prime * result + ((regex == null) ? 0 : regex.hashCode());
        result = prime * result + regexSelectCaret;
        result = prime * result + regexSelectEnd;
        result = prime * result + regexSelectStart;
        result = prime * result + ((selectionRegex == null) ? 0 : selectionRegex.hashCode());
        result = prime * result + ((target == null) ? 0 : target.hashCode());
        result = prime * result + targetEnd;
        result = prime * result + targetStart;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        UiState other = (UiState) obj;
        if (highlightGroup != other.highlightGroup)
            return false;
        if (highlightGroupNumber != other.highlightGroupNumber)
            return false;
        if (highlightSelection != other.highlightSelection)
            return false;
        if (matchIndex != other.matchIndex)
            return false;
        if (patternFlags != other.patternFlags)
            return false;
        if (regex == null) {
            if (other.regex != null)
                return false;
        }
        else if (!regex.equals(other.regex))
            return false;
        if (regexSelectCaret != other.regexSelectCaret)
            return false;
        if (regexSelectEnd != other.regexSelectEnd)
            return false;
        if (regexSelectStart != other.regexSelectStart)
            return false;
        if (selectionRegex == null) {
            if (other.selectionRegex != null)
                return false;
        }
        else if (!selectionRegex.equals(other.selectionRegex))
            return false;
        if (target == null) {
            if (other.target != null)
                return false;
        }
        else if (!target.equals(other.target))
            return false;
        if (targetEnd != other.targetEnd)
            return false;
        if (targetStart != other.targetStart)
            return false;
        return true;
    }
}

class RegexHighlighter extends KeyAdapter implements ActionListener, ChangeListener, CaretListener {
    //Deal with concurrent UI updates
    final AtomicBoolean doUpdate = new AtomicBoolean(false);
    final AtomicBoolean inHighlights = new AtomicBoolean(false);
    volatile UiState lastUiState = null;
    
    def swing // reference to the view
    final def orange = new DefaultHighlighter.DefaultHighlightPainter(Color.ORANGE)
    final def yellow = new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW)
    final def red = new DefaultHighlighter.DefaultHighlightPainter(Color.RED)
    final def gray = new DefaultHighlighter.DefaultHighlightPainter(Color.GRAY)

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
        doHighlights()
    }

    // the main regex logic
    public doHighlights() {
        doUpdate.set(true);
        if (!inHighlights.compareAndSet(false, true)) {
            return;
        }
        try {
            while (doUpdate.compareAndSet(true, false)) {
                def currentState = new UiState(swing);
                if (currentState.equals(lastUiState)) {
                    //Nothing changed since the last update, don't bother doing anything
                    return;
                }
                def prevUiState = lastUiState;
                lastUiState = currentState;

                //trim the target string
                def target = currentState.target.substring(currentState.targetStart, currentState.targetEnd);

                //There is a regex and a target
                if (currentState.regex.length() > 0 && target.length() > 0) {
                    //create the pattern & matcher
                    def pattern = Pattern.compile(currentState.regex, currentState.patternFlags);
                    def matcher = pattern.matcher(target);
                    
                    //Count the number of matches
                    while (matcher.find()) {
                        currentState.matchCount++;
                    }
                    matcher.reset();
                    
                    //Find the correct match
                    for (def match = 0; match < currentState.matchIndex - 1; match++) {
                        matcher.find();
                    }
                    if (matcher.find()) {
                        //Do matching of the regex selection
                        def selectionMatcher = null;
                        if (currentState.selectionRegex != null) {
                            try {
                                def selectionPattern = Pattern.compile(currentState.selectionRegex, currentState.patternFlags);
                                selectionMatcher = selectionPattern.matcher(matcher.group(0));
                                currentState.selectionMatched = selectionMatcher.find()
                            }
                            catch (Exception e) {
                            }
                        }
                        
                        //Record group count
                        currentState.groupCount = matcher.groupCount();
                        
                        //Record match
                        currentState.matchStart = currentState.targetStart + matcher.start();
                        currentState.matchEnd = currentState.targetStart + matcher.end();                        

                        //Record highlight
                        if (currentState.highlightSelection && currentState.selectionMatched) {
                            currentState.highlightStart = currentState.matchStart + selectionMatcher.start();
                            currentState.highlightEnd = currentState.matchStart + selectionMatcher.end();  
                        }
                        else if (currentState.highlightGroup) {
                            currentState.highlightStart = currentState.targetStart + matcher.start(currentState.highlightGroupNumber);
                            currentState.highlightEnd = currentState.targetStart + matcher.end(currentState.highlightGroupNumber);                        
                        }
                    }
                }
                
                //Remove all highlights
                swing.regexPane.highlighter.removeAllHighlights()
                swing.targetPane.highlighter.removeAllHighlights()
                swing.regexStatus.text = " "
                swing.targetStatus.text = " "
                
                //Add the user's selections back after clearing the highlights
                if (currentState.regexSelectStart < currentState.regexSelectEnd) {
                    if (currentState.regexSelectCaret == currentState.regexSelectStart) {
                        swing.regexPane.caret.setDot(currentState.regexSelectEnd);
                        swing.regexPane.caret.moveDot(currentState.regexSelectStart);
                    }
                    else {
                        swing.regexPane.select(currentState.regexSelectStart, currentState.regexSelectEnd);
                    }
                    swing.regexPane.caret.setSelectionVisible(true);
                    
                }
                if (currentState.targetSelectStart < currentState.targetSelectEnd) {
                    if (currentState.targetSelectCaret == currentState.targetSelectStart) {
                        swing.targetPane.caret.setDot(currentState.targetSelectEnd);
                        swing.targetPane.caret.moveDot(currentState.targetSelectStart);
                    }
                    else {
                        swing.targetPane.select(currentState.targetSelectStart, currentState.targetSelectEnd);
                    }
                    swing.targetPane.caret.setSelectionVisible(true);
                }
                
                //Update endOfString spinner
                def targetStartMax = Math.max(currentState.targetEnd, 0);
                swing.endOfString.model.maximum = currentState.target.length();
                if (prevUiState == null || prevUiState.target.length() <= currentState.targetEnd) {                
                    swing.endOfString.value = currentState.target.length();
                    targetStartMax = currentState.target.length();
                }
                swing.endOfString.model.minimum = currentState.targetStart;
                
                //Update startOfString spinner
                swing.startOfString.model.maximum = targetStartMax;
                if (currentState.targetStart > targetStartMax) {
                    swing.startOfString.value = targetStartMax;
                }

                //Count matches & update match spinner
                swing.matchNumber.model.maximum = currentState.matchCount;
                swing.matchCount.text = currentState.matchCount;
                if (currentState.matchCount > 0 && currentState.matchIndex > currentState.matchCount) {
                    swing.matchNumber.value = currentState.matchCount
                }
                
                swing.highlightSelection.enabled = currentState.selectionMatched;
                    
                //Update group count spinner
                swing.highlightGroupNumber.model.maximum = currentState.groupCount;
                swing.groupCount.text = currentState.groupCount;
                if (currentState.highlightGroupNumber > currentState.groupCount) {
                    swing.highlightGroupNumber.value = currentState.groupCount
                }
            
                //Highlight the areas that are being ignored
                swing.targetPane.highlighter.addHighlight(0, currentState.targetStart, gray);
                swing.targetPane.highlighter.addHighlight(currentState.targetEnd, currentState.target.length(), gray);
                
                if (currentState.highlightStart != currentState.highlightEnd) {
                    swing.targetPane.highlighter.addHighlight(currentState.highlightStart, currentState.highlightEnd, orange)
                }
                
                if (currentState.matchStart != currentState.matchEnd) {
                    swing.targetPane.highlighter.addHighlight(currentState.matchStart, currentState.matchEnd, yellow)
                    swing.targetStatus.text = "Match #${currentState.matchIndex} from ${currentState.matchStart} to ${currentState.matchEnd}."
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
