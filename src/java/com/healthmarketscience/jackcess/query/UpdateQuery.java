/*
Copyright (c) 2008 Health Market Science, Inc.

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

package com.healthmarketscience.jackcess.query;

import java.util.List;

import static com.healthmarketscience.jackcess.query.QueryFormat.*;


/**
 * @author James Ahlborn
 */
public class UpdateQuery extends Query 
{

  public UpdateQuery(String name, List<Row> rows, int objectId) {
    super(name, rows, objectId, Type.UPDATE);
  }

  public List<String> getTargetTables() 
  {
    return super.getFromTables();
  }

  public String getRemoteDb() 
  {
    return super.getFromRemoteDb();
  }

  public List<String> getNewValues()
  {
    return (new RowFormatter(getColumnRows()) {
        @Override protected void format(StringBuilder builder, Row row) {
          toOptionalQuotedExpr(builder, row.name2, true)
            .append(" = ").append(row.expression);
        }
      }).format();
  }

  @Override
  public String getWhereExpression()
  {
    return super.getWhereExpression();
  }

  @Override
  protected void toSQLString(StringBuilder builder)
  {
    builder.append("UPDATE ").append(getTargetTables());
    toRemoteDb(builder, getRemoteDb());

    builder.append(NEWLINE).append("SET ").append(getNewValues());

    String whereExpr = getWhereExpression();
    if(whereExpr != null) {
      builder.append(NEWLINE).append("WHERE ").append(whereExpr);
    }
  }  

}
