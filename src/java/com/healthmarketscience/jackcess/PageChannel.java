/*
Copyright (c) 2005 Health Market Science, Inc.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
USA

You can contact Health Market Science at info@healthmarketscience.com
or at the following address:

Health Market Science
2700 Horizon Drive
Suite 200
King of Prussia, PA 19406
*/

package com.healthmarketscience.jackcess;

import java.io.Flushable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Reads and writes individual pages in a database file
 * @author Tim McCune
 */
public class PageChannel implements Channel, Flushable {
  
  private static final Log LOG = LogFactory.getLog(PageChannel.class);
  
  static final int INVALID_PAGE_NUMBER = -1;

  /** dummy buffer used when allocating new pages */
  private static final ByteBuffer FORCE_BYTES = ByteBuffer.allocate(1);
  
  /** Global usage map always lives on page 1 */
  private static final int PAGE_GLOBAL_USAGE_MAP = 1;
  
  /** Channel containing the database */
  private final FileChannel _channel;
  /** Format of the database in the channel */
  private final JetFormat _format;
  /** whether or not to force all writes to disk immediately */
  private final  boolean _autoSync;
  /** Tracks free pages in the database. */
  private UsageMap _globalUsageMap;
  
  /**
   * @param channel Channel containing the database
   * @param format Format of the database in the channel
   */
  public PageChannel(FileChannel channel, JetFormat format, boolean autoSync)
    throws IOException
  {
    _channel = channel;
    _format = format;
    _autoSync = autoSync;
  }

  /**
   * Does second-stage initialization, must be called after construction.
   */
  public void initialize(Database database)
    throws IOException
  {
    // note the global usage map is a special map where any page outside of
    // the current range is assumed to be "on"
    _globalUsageMap = UsageMap.read(database, PAGE_GLOBAL_USAGE_MAP, (byte) 0,
                                    true);
  }
  
  /**
   * Only used by unit tests
   */
  PageChannel(boolean testing) {
    if(!testing) {
      throw new IllegalArgumentException();
    }
    _channel = null;
    _format = JetFormat.VERSION_4;
    _autoSync = false;
  }

  public JetFormat getFormat() {
    return _format;
  }
  
  /**
   * @param buffer Buffer to read the page into
   * @param pageNumber Number of the page to read in (starting at 0)
   * @return True if the page was successfully read into the buffer, false if
   *    that page doesn't exist.
   */
  public boolean readPage(ByteBuffer buffer, int pageNumber)
    throws IOException
  {
    if(pageNumber == INVALID_PAGE_NUMBER) {
      throw new IllegalStateException("invalid page number");
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("Reading in page " + Integer.toHexString(pageNumber));
    }
    buffer.clear();
    boolean rtn = _channel.read(buffer, (long) pageNumber * (long) getFormat().PAGE_SIZE) != -1;
    buffer.flip();
    return rtn;
  }
  
  /**
   * Write a page to disk
   * @param page Page to write
   * @param pageNumber Page number to write the page to
   */
  public void writePage(ByteBuffer page, int pageNumber) throws IOException {
    writePage(page, pageNumber, 0);
  }
  
  /**
   * Write a page (or part of a page) to disk
   * @param page Page to write
   * @param pageNumber Page number to write the page to
   * @param pageOffset offset within the page at which to start writing the
   *                   page data
   */
  public void writePage(ByteBuffer page, int pageNumber,
                        int pageOffset)
    throws IOException
  {
    page.rewind();
    page.position(pageOffset);
    _channel.write(page, (((long) pageNumber * (long) getFormat().PAGE_SIZE) +
                          (long) pageOffset));
    if(_autoSync) {
      flush();
    }
  }
  
  /**
   * Write a page to disk as a new page, appending it to the database
   * @param page Page to write
   * @return Page number at which the page was written
   */
  public int writeNewPage(ByteBuffer page) throws IOException
  {
    long size = _channel.size();
    page.rewind();
    // push the buffer to the end of the page, so that a full page's worth of
    // data is written regardless of the incoming buffer size (we use a tiny
    // buffer in allocateNewPage)
    long offset = size + (getFormat().PAGE_SIZE - page.remaining());
    _channel.write(page, offset);
    int pageNumber = (int) (size / getFormat().PAGE_SIZE);
    _globalUsageMap.removePageNumber(pageNumber);  //force is done here
    return pageNumber;
  }

  /**
   * Allocates a new page in the database.  Data in the page is undefined
   * until it is written in a call to {@link #writePage}.
   */
  public int allocateNewPage() throws IOException {
    // this will force the file to be extended with mostly undefined bytes
    return writeNewPage(FORCE_BYTES);
  }
  
  /**
   * @return A newly-allocated buffer that can be passed to readPage
   */
  public ByteBuffer createPageBuffer() {
    return createBuffer(getFormat().PAGE_SIZE);
  }

  /**
   * @return A newly-allocated buffer of the given size and LITTLE_ENDIAN byte
   *         order
   */
  public ByteBuffer createBuffer(int size) {
    return createBuffer(size, ByteOrder.LITTLE_ENDIAN);
  }
  
  /**
   * @return A newly-allocated buffer of the given size and byte order
   */
  public ByteBuffer createBuffer(int size, ByteOrder order) {
    ByteBuffer rtn = ByteBuffer.allocate(size);
    rtn.order(order);
    return rtn;
  }
  
  public void flush() throws IOException {
    _channel.force(true);
  }
  
  public void close() throws IOException {
    flush();
    _channel.close();
  }
  
  public boolean isOpen() {
    return _channel.isOpen();
  }

  /**
   * @return a duplicate of the current buffer narrowed to the given position
   *         and limit.  mark will be set at the current position.
   */
  public static ByteBuffer narrowBuffer(ByteBuffer buffer, int position,
                                        int limit)
  {
    return (ByteBuffer)buffer.duplicate()
      .order(buffer.order())
      .clear()
      .limit(limit)
      .position(position)
      .mark();
  }
  
}
