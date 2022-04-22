package com.tyron.builder.api.file;

import com.tyron.builder.api.Action;
import com.tyron.builder.api.tasks.util.PatternFilterable;

import java.io.File;
import java.util.Set;

/**
 * <p>A {@code FileTree} represents a hierarchy of files. It extends {@link FileCollection} to add hierarchy query and
 * manipulation methods. You typically use a {@code FileTree} to represent files to copy or the contents of an
 * archive.</p>
 *
 * <p>You can obtain a {@code FileTree} instance using {@link org.gradle.api.Project#fileTree(java.util.Map)},
 * {@link org.gradle.api.Project#zipTree(Object)} or {@link org.gradle.api.Project#tarTree(Object)}.
 * </p>
 *
 * <p>The order of the files in a {@code FileTree} is not stable, even on a single computer.
 */
public interface FileTree extends FileCollection {
//    /**
//     * <p>Restricts the contents of this tree to those files matching the given filter. The filtered tree is live, so
//     * that any changes to this tree are reflected in the filtered tree.</p>
//     *
//     * <p>The given closure is used to configure the filter. A {@link org.gradle.api.tasks.util.PatternFilterable} is
//     * passed to the closure as its delegate. Only files which match the specified include patterns will be included in
//     * the filtered tree. Any files which match the specified exclude patterns will be excluded from the filtered
//     * tree.</p>
//     *
//     * @param filterConfigClosure the closure to use to configure the filter.
//     * @return The filtered tree.
//     */
//    FileTree matching(Closure filterConfigClosure);

    /**
     * <p>Restricts the contents of this tree to those files matching the given filter. The filtered tree is live, so
     * that any changes to this tree are reflected in the filtered tree.</p>
     *
     * <p>The given action is used to configure the filter. A {@link org.gradle.api.tasks.util.PatternFilterable} is
     * passed to the action. Only files which match the specified include patterns will be included in
     * the filtered tree. Any files which match the specified exclude patterns will be excluded from the filtered
     * tree.</p>
     *
     * @param filterConfigAction Action to use to configure the filter.
     * @return The filtered tree.
     * @since 3.3
     */
    FileTree matching(Action<? super PatternFilterable> filterConfigAction);

    /**
     * <p>Restricts the contents of this tree to those files matching the given filter. The filtered tree is live, so
     * that any changes to this tree are reflected in the filtered tree.</p>
     *
     * <p>The given pattern set is used to configure the filter. Only files which match the specified include patterns
     * will be included in the filtered tree. Any files which match the specified exclude patterns will be excluded from
     * the filtered tree.</p>
     *
     * @param patterns the pattern set to use to configure the filter.
     * @return The filtered tree.
     */
    FileTree matching(PatternFilterable patterns);

    /**
     * Visits the files and directories in this file tree. Files are visited in depth-first prefix order, so that a directory
     * is visited before its children.
     *
     * @param visitor The visitor.
     * @return this
     */
    FileTree visit(FileVisitor visitor);

//    /**
//     * Visits the files and directories in this file tree. Files are visited in depth-first prefix order, so that a directory
//     * is visited before its children. The file/directory to be visited is passed to the given closure as a {@link
//     * FileVisitDetails}
//     *
//     * @param visitor The visitor.
//     * @return this
//     */
//    FileTree visit(Closure visitor);

    /**
     * Visits the files and directories in this file tree. Files are visited in depth-first prefix order, so that a directory
     * is visited before its children. The file/directory to be visited is passed to the given action as a {@link
     * FileVisitDetails}
     *
     * @param visitor The visitor.
     * @return this
     */
    FileTree visit(Action<? super FileVisitDetails> visitor);

    /**
     * Returns a {@code FileTree} which contains the union of this tree and the given tree. The returned tree is live,
     * so that changes to either this tree or the other source tree are reflected in the returned tree.
     *
     * @param fileTree The tree. Should not be null.
     * @return The union of this tree and the given tree.
     */
    FileTree plus(FileTree fileTree);

    /**
     * Returns this.
     *
     * @return this
     */
    @Override
    FileTree getAsFileTree();

    /**
     * Returns the contents of this tree as a flattened Set.
     *
     * <p>The order of the files in a {@code FileTree} is not stable, even on a single computer.
     *
     * @return The files. Returns an empty set if this tree is empty.
     */
    @Override
    Set<File> getFiles();
}