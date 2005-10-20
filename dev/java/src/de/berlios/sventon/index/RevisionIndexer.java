package de.berlios.sventon.index;

import de.berlios.sventon.ctrl.RepositoryConfiguration;
import de.berlios.sventon.ctrl.RepositoryEntry;
import de.berlios.sventon.svnsupport.RepositoryFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.io.SVNRepository;

import java.io.*;
import java.util.*;

/**
 * RevisionIndexer.
 *
 * @author jesper@users.berlios.de
 */
public class RevisionIndexer {

  /**
   * The revision index.
   */
  private RevisionIndex index;

  /**
   * The repository instance to index.
   */
  private SVNRepository repository;

  /**
   * The repository configuration.
   */
  private RepositoryConfiguration configuration;

  /**
   * The logging instance.
   */
  private final Log logger = LogFactory.getLog(getClass());

  public static final String INDEX_FILENAME = "index.ser";

  /**
   * Constructs the index instance using a given repository.
   * The repository must be correctly initialized and credentials
   * must have been applied (if required by server).
   *
   * @param repository The repository instance
   */
  public RevisionIndexer(final SVNRepository repository) throws SVNException {
    logger.debug("Creating index instance using given repository");
    this.repository = repository;
    index = new RevisionIndex();
  }

  /**
   * Constructs the index instance using a given repository configuration.
   *
   * @param configuration The repository configuration
   */
  public RevisionIndexer(final RepositoryConfiguration configuration) throws SVNException {
    logger.debug("Creating index instance using given configuration");
    this.configuration = configuration;
    logger.debug("Creating the repository instance");
    repository = RepositoryFactory.INSTANCE.getRepository(configuration, true);
    if (repository == null) {
      logger.warn("Repository not configured - unable to create index");
      return;
    }
    initIndex();
  }

  /**
   * @return Returns the number of indexed repository items.
   */
  public long getIndexCount() {
    return index.getEntries().size();
  }

  /**
   * Gets the interator for index access.
   *
   * @return The iterator.
   */
  public Iterator<RepositoryEntry> getEntriesIterator() {
    return index.getEntries().iterator();
  }

  /**
   * Initializes the index.
   * If a serialized index is stored on disk it will be read into memory.
   * Otherwise a complete repository indexing will be executed.
   */
  private void initIndex() {
    logger.debug("Initializing index");

  // TODO: Update the index according to what's new.

    logger.info("Reading serialized index from disk, "
        + configuration.getSVNConfigurationPath()
        + INDEX_FILENAME);
    ObjectInputStream in = null;
    try {
      in = new ObjectInputStream(new FileInputStream(configuration.getSVNConfigurationPath() + INDEX_FILENAME));
      index = (RevisionIndex) in.readObject();
    } catch (ClassNotFoundException e) {
      logger.warn(e);
    } catch (IOException e) {
      logger.warn(e);
    }

    // No serialized index excisted - initialize an empty one.
    if (index == null) {
      index = new RevisionIndex();
    }

  }

  /**
   * Checks if index is dirty or up to date by comparing
   * revision and making sure repository has not changed since
   * last index update.
   *
   * @return <code>True</code> if index is concidered dirty and should be reindexed.
   * @throws SVNException if a Subversion error occurs.
   */
  public boolean isDirty() throws SVNException {
    boolean dirty = index.getIndexRevision() != this.repository.getLatestRevision();
    return dirty && index.getUrl().equals(this.repository.getLocation().toString());
  }

  /**
   * Indexes the files and directories, starting at the path
   * specified by calling <code>setStartPath()</code>.
   *
   * @throws SVNException if a Subversion error occurs.
   */
  public void index() throws SVNException {
    logger.info("Building index");
    index.clearIndex();
    index.setIndexRevision(this.repository.getLatestRevision());
    logger.debug("Revision: " + index.getIndexRevision());
    index.setUrl(this.repository.getLocation().toString());
    logger.debug("Index url: " + index.getUrl());
    populateIndex("/");   // TODO: Use mount point here!
    logger.info("Number of indexed entries: " + getIndexCount());
  }

  /**
   * Populates the index by getting all entries in given path
   * and adding them to the index. This method will be recursively
   * called by <code>index()</code>.
   *
   * @param path The path to add to index.
   * @throws SVNException if a Subversion error occurs.
   */
  @SuppressWarnings("unchecked")
  private void populateIndex(final String path) throws SVNException {
    List<SVNDirEntry> entriesList = Collections.checkedList(new ArrayList<SVNDirEntry>(), SVNDirEntry.class);

    entriesList.addAll(repository.getDir(path, index.getIndexRevision(), new HashMap(), (Collection) null));
    for (SVNDirEntry entry : entriesList) {
      index.add(new RepositoryEntry(entry, path));
      if (entry.getKind() == SVNNodeKind.DIR) {
        populateIndex(path + entry.getName() + "/");
      }
    }
  }

  /**
   * Finds index entries by a search string.
   *
   * @param searchString The string to search for.
   * @return The <code>List</code> of <code>IndexEntry</code> instances found.
   * @throws SVNException if a Subverions error occurs.
   * @see de.berlios.sventon.ctrl.RepositoryEntry
   */
  public List<RepositoryEntry> find(final String searchString) throws SVNException {
    if (searchString == null || searchString.equals("")) {
      throw new IllegalArgumentException("Search string was null or empty");
    }

    //TODO: Temp fix until index refreshing code is added.
    if (isDirty()) {
      index();
    }

    List<RepositoryEntry> result = Collections.checkedList(new ArrayList<RepositoryEntry>(), RepositoryEntry.class);
    for (RepositoryEntry entry : index.getEntries()) {
      if (entry.getFullEntryName().toLowerCase().indexOf(searchString.toLowerCase()) > -1) {
        result.add(entry);
      }
    }
    logger.debug("Found " + result.size() + " entries matching search: " + searchString);
    return result;
  }

  /**
   * Finds index entries by a search string.
   *
   * @param searchPattern The regex pattern to search for.
   * @return The <code>List</code> of entries found.
   * @throws SVNException if a Subverions error occurs.
   * @see java.util.regex.Pattern
   */
  public List findPattern(final String searchPattern) throws SVNException {
    if (searchPattern == null || searchPattern.equals("")) {
      throw new IllegalArgumentException("Search string was null or empty");
    }

    //TODO: Temp fix until index refreshing code is added.
    if (isDirty()) {
      index();
    }

    List<RepositoryEntry> result = Collections.checkedList(new ArrayList<RepositoryEntry>(), RepositoryEntry.class);
    for (RepositoryEntry entry : index.getEntries()) {
      if (entry.getFullEntryName().matches(searchPattern)) {
        result.add(entry);
      }
    }
    logger.debug("Found " + result.size() + " entries matching search: " + searchPattern);
    return result;
  }

  /**
   * Gets all subdirectory entries below given <code>fromPath</code>.
   *
   * @param fromPath The base path to start from.
   * @return A list containing all subdirectory entries below <code>fromPath</code>.
   * @throws SVNException if a Subverions error occurs.
   */
  public List<RepositoryEntry> getDirectories(final String fromPath) throws SVNException {
    if (fromPath == null || fromPath.equals("")) {
      throw new IllegalArgumentException("Path was null or empty");
    }

    //TODO: Temp fix until index refreshing code is added.
    if (isDirty()) {
      index();
    }

    List<RepositoryEntry> result = Collections.checkedList(new ArrayList<RepositoryEntry>(), RepositoryEntry.class);
    for (RepositoryEntry entry : index.getEntries()) {
      if ("dir".equals(entry.getKind()) && entry.getFullEntryName().startsWith(fromPath)) {
        result.add(entry);
      }
    }
    logger.debug("Found " + result.size() + " directories below: " + fromPath);
    return result;
  }

  /**
   * This method serializes the index to disk.
   */
  public void destroy() {
    logger.info("Saving index to disk, " + configuration.getSVNConfigurationPath() + INDEX_FILENAME);
    ObjectOutputStream out = null;
    try {
      out = new ObjectOutputStream(new FileOutputStream(configuration.getSVNConfigurationPath() + INDEX_FILENAME));
      out.writeObject(index);
    } catch (IOException e) {
      logger.warn(e);
    }
  }
}
