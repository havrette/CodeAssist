package com.tyron.completion.impl;

import com.tyron.completion.CompletionInitializationContext;
import com.tyron.completion.EditorMemory;
import com.tyron.completion.InsertionContext;
import com.tyron.completion.OffsetMap;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.kotlin.com.intellij.lang.FileASTNode;
import org.jetbrains.kotlin.com.intellij.lang.injection.InjectedLanguageManager;
import org.jetbrains.kotlin.com.intellij.openapi.diagnostic.Attachment;
import org.jetbrains.kotlin.com.intellij.openapi.diagnostic.RuntimeExceptionWithAttachments;
import org.jetbrains.kotlin.com.intellij.openapi.editor.Document;
import org.jetbrains.kotlin.com.intellij.openapi.util.TextRange;
import org.jetbrains.kotlin.com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.kotlin.com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.kotlin.com.intellij.psi.FileViewProvider;
import org.jetbrains.kotlin.com.intellij.psi.PsiDocumentManager;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiFile;
import org.jetbrains.kotlin.com.intellij.psi.impl.DebugUtil;
import org.jetbrains.kotlin.com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.kotlin.com.intellij.util.text.ImmutableCharSequence;

import io.github.rosemoe.sora.widget.CodeEditor;

public class CompletionAssertions {

    static void assertCommitSuccessful(CodeEditor editor, PsiFile psiFile) {
        Document document = EditorMemory.getUserData(editor, EditorMemory.DOCUMENT_KEY);
        int docLength = document.getTextLength();
        int psiLength = psiFile.getTextLength();
        PsiDocumentManager manager = PsiDocumentManager.getInstance(psiFile.getProject());
        boolean committed = !manager.isUncommited(document);
        if (docLength == psiLength && committed) {
            return;
        }

        FileViewProvider viewProvider = psiFile.getViewProvider();

        String message = "unsuccessful commit:";
        message += "\nmatching=" + (psiFile == manager.getPsiFile(document));
        message += "\ninjectedEditor=" + false;
        message += "\ninjectedFile=" + InjectedLanguageManager.getInstance(psiFile.getProject()).isInjectedFragment(psiFile);
        message += "\ncommitted=" + committed;
        message += "\nfile=" + psiFile.getName();
        message += "\nfile class=" + psiFile.getClass();
        message += "\nfile.valid=" + psiFile.isValid();
        message += "\nfile.physical=" + psiFile.isPhysical();
        message += "\nfile.eventSystemEnabled=" + viewProvider.isEventSystemEnabled();
        message += "\nlanguage=" + psiFile.getLanguage();
        message += "\ndoc.length=" + docLength;
        message += "\npsiFile.length=" + psiLength;
        String fileText = psiFile.getText();
        if (fileText != null) {
            message += "\npsiFile.text.length=" + fileText.length();
        }
        FileASTNode node = psiFile.getNode();
        if (node != null) {
            message += "\nnode.length=" + node.getTextLength();
            String nodeText = node.getText();
            message += "\nnode.text.length=" + nodeText.length();
        }
        VirtualFile virtualFile = viewProvider.getVirtualFile();
        message += "\nvirtualFile=" + virtualFile;
        message += "\nvirtualFile.class=" + virtualFile.getClass();
        message += "\n" + DebugUtil.currentStackTrace();

        throw new RuntimeExceptionWithAttachments(
                "Commit unsuccessful", message,
                new Attachment(virtualFile.getPath() + "_file.txt", StringUtil.notNullize(fileText)),
                createAstAttachment(psiFile, psiFile),
                new Attachment("docText.txt", document.getText()));
    }

    static void checkEditorValid(CodeEditor editor) {
        if (!isEditorValid(editor)) {
            throw new AssertionError();
        }
    }

    static boolean isEditorValid(CodeEditor editor) {
        return editor.isAttachedToWindow();
//        return !(editor instanceof EditorWindow) || ((EditorWindow)editor).isValid();
    }

    private static Attachment createAstAttachment(PsiFile fileCopy, final PsiFile originalFile) {
        return new Attachment(originalFile.getViewProvider().getVirtualFile().getPath() + " syntactic tree.txt", DebugUtil.psiToString(fileCopy,
                true, true));
    }

    private static Attachment createFileTextAttachment(PsiFile fileCopy, final PsiFile originalFile) {
        return new Attachment(originalFile.getViewProvider().getVirtualFile().getPath(), fileCopy.getText());
    }

//    static void assertInjectedOffsets(int hostStartOffset, PsiFile injected, DocumentWindow documentWindow) {
//        assert documentWindow != null : "no DocumentWindow for an injected fragment";
//
//        InjectedLanguageManager injectedLanguageManager = InjectedLanguageManager.getInstance(injected.getProject());
//        TextRange injectedRange = injected.getTextRange();
//        int hostMinOffset = injectedLanguageManager.injectedToHost(injected, injectedRange.getStartOffset(), true);
//        int hostMaxOffset = injectedLanguageManager.injectedToHost(injected, injectedRange.getEndOffset(), false);
//        assert hostStartOffset >= hostMinOffset : "startOffset before injected";
//        assert hostStartOffset <= hostMaxOffset : "startOffset after injected";
//    }

    static void assertHostInfo(PsiFile hostCopy, OffsetMap hostMap) {
        PsiUtilCore.ensureValid(hostCopy);
        if (hostMap.getOffset(CompletionInitializationContext.START_OFFSET) > hostCopy.getTextLength()) {
            throw new AssertionError("startOffset outside the host file: " + hostMap.getOffset(CompletionInitializationContext.START_OFFSET) + "; " + hostCopy);
        }
    }

    @Contract("_,_,_,null->fail")
    static void assertCompletionPositionPsiConsistent(OffsetsInFile offsets,
                                                      int offset,
                                                      PsiFile originalFile, PsiElement insertedElement) {
        PsiFile fileCopy = offsets.getFile();
        if (insertedElement == null) {
            throw new RuntimeExceptionWithAttachments(
                    "No element at insertion offset",
                    "offset=" + offset,
                    createFileTextAttachment(fileCopy, originalFile),
                    createAstAttachment(fileCopy, originalFile));
        }

        final TextRange range = insertedElement.getTextRange();
        CharSequence fileCopyText = fileCopy.getViewProvider().getContents();
        if ((range.getEndOffset() > fileCopyText.length()) ||
            !isEquals(fileCopyText.subSequence(range.getStartOffset(), range.getEndOffset()),
                    insertedElement.getNode().getChars())) {
            throw new RuntimeExceptionWithAttachments(
                    "Inconsistent completion tree",
                    "range=" + range + "; fileLength=" + fileCopyText.length(),
                    createFileTextAttachment(fileCopy, originalFile),
                    createAstAttachment(fileCopy, originalFile),
                    new Attachment("Element at caret.txt", insertedElement.getText()));
        }
    }

    private static boolean isEquals(CharSequence left, CharSequence right) {
        if (left == right) return true;
        if (left instanceof ImmutableCharSequence && right instanceof ImmutableCharSequence) {
            return left.equals(right);
        }
        return left.toString().equals(right.toString());
    }

    static void assertCorrectOriginalFile(@NonNls String prefix, PsiFile file, PsiFile copy) {
        if (copy.getOriginalFile() != file) {
            throw new AssertionError(prefix + " copied file doesn't have correct original: noOriginal=" + (copy.getOriginalFile() == copy) +
                                     "\n file " + fileInfo(file) +
                                     "\n copy " + fileInfo(copy));
        }
    }

    private static @NonNls String fileInfo(PsiFile file) {
        return file + " of " + file.getClass() +
               " in " + file.getViewProvider() + ", languages=" + file.getViewProvider().getLanguages() +
               ", physical=" + file.isPhysical();
    }

//    static class WatchingInsertionContext extends InsertionContext implements Disposable {
//        private RangeMarkerEx tailWatcher;
//        Throwable invalidateTrace;
//        DocumentEvent killer;
//        private RangeMarkerSpy spy;
//
//        WatchingInsertionContext(OffsetMap offsetMap, PsiFile file, char completionChar, List<LookupElement> items, Editor editor) {
//            super(offsetMap, completionChar, items.toArray(LookupElement.EMPTY_ARRAY),
//                    file, editor,
//                    shouldAddCompletionChar(completionChar));
//        }
//
//        @Override
//        public void setTailOffset(int offset) {
//            super.setTailOffset(offset);
//            watchTail(offset);
//        }
//
//        private void watchTail(int offset) {
//            stopWatching();
//            tailWatcher = (RangeMarkerEx)getDocument().createRangeMarker(offset, offset);
//            if (!tailWatcher.isValid()) {
//                throw new AssertionError(getDocument() + "; offset=" + offset);
//            }
//            tailWatcher.setGreedyToRight(true);
//            spy = new RangeMarkerSpy(tailWatcher) {
//                @Override
//                protected void invalidated(DocumentEvent e) {
//                    if (invalidateTrace == null) {
//                        invalidateTrace = new Throwable();
//                        killer = e;
//                    }
//                }
//            };
//            getDocument().addDocumentListener(spy);
//        }
//
//        void stopWatching() {
//            if (tailWatcher != null) {
//                getDocument().removeDocumentListener(spy);
//                tailWatcher.dispose();
//            }
//        }
//
//        @Override
//        public int getTailOffset() {
//            if (!getOffsetMap().containsOffset(TAIL_OFFSET) && invalidateTrace != null) {
//                throw new RuntimeExceptionWithAttachments("Tail offset invalid", new Attachment("invalidated", invalidateTrace));
//            }
//
//            int offset = super.getTailOffset();
//            if (tailWatcher.getStartOffset() != tailWatcher.getEndOffset() && offset > 0) {
//                watchTail(offset);
//            }
//
//            return offset;
//        }
//
//        @Override
//        public void dispose() {
//            stopWatching();
//        }
//    }
}