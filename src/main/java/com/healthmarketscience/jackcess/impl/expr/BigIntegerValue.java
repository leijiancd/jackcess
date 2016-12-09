/*
Copyright (c) 2016 James Ahlborn

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.healthmarketscience.jackcess.impl.expr;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 *
 * @author James Ahlborn
 */
public class BigIntegerValue extends BaseNumericValue
{
  private final BigInteger _val;

  public BigIntegerValue(BigInteger val) 
  {
    _val = val;
  }

  public Type getType() {
    return Type.BIG_INT;
  }

  public Object get() {
    return _val;
  }

  @Override
  protected Number getNumber() {
    return _val;
  }

  @Override
  public boolean getAsBoolean() {
    return (_val.compareTo(BigInteger.ZERO) != 0L);
  }

  @Override
  public BigInteger getAsBigInteger() {
    return _val;
  }

  @Override
  public BigDecimal getAsBigDecimal() {
    return new BigDecimal(_val);
  }
}
