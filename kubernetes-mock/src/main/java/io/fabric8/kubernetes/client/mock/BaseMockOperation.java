/**
 * Copyright (C) 2015 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fabric8.kubernetes.client.mock;

import io.fabric8.kubernetes.api.model.Doneable;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.Client;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.ClientNonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.ClientResource;
import io.fabric8.kubernetes.client.dsl.EditReplaceDeletable;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.ClientMixedOperation;
import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;
import org.easymock.IExpectationSetters;
import org.easymock.internal.matchers.And;

import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.fabric8.kubernetes.client.mock.util.MockUtils.getArgument;
import static org.easymock.EasyMock.expect;

public class BaseMockOperation<C extends Client, T, L extends KubernetesResourceList, D extends Doneable<T>, B extends MockDoneable<T>,
  R extends ClientResource<T, D>,
  E extends Resource<T, IExpectationSetters<T>, B, IExpectationSetters<Boolean>>>
  implements MockOperation<T, L, B, E>,
  MockResource<T, B, Boolean>,
  Mockable {

  private final ClientMixedOperation<C, T, L, D, R> delegate;
  private final Set<Mockable> nested = new LinkedHashSet<>();
  private final Class mockDoneableType;

  public BaseMockOperation() {
    this(EasyMock.createMock(ClientMixedOperation.class));
  }

  public BaseMockOperation(ClientMixedOperation delegate) {
    this.delegate = delegate;
    this.mockDoneableType = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[4];
  }


  private BaseMockOperation loadedMockOp;
  private BaseMockOperation allNamespacesOp;
  private Map<IArgumentMatcher, BaseMockOperation> nameMap = new HashMap<>();
  private Map<IArgumentMatcher, BaseMockOperation> namespaceMap = new HashMap<>();
  private Map<IArgumentMatcher, BaseMockOperation> cascadingMap = new HashMap<>();

  private Map<IArgumentMatcher, BaseMockOperation> labelMap = new HashMap<>();
  private Map<IArgumentMatcher, BaseMockOperation> labelNotMap = new HashMap<>();
  private Map<IArgumentMatcher, BaseMockOperation> labelsMap = new HashMap<>();
  private Map<IArgumentMatcher, BaseMockOperation> labelsNotMap = new HashMap<>();
  private Map<IArgumentMatcher, BaseMockOperation> labelInsMap = new HashMap<>();
  private Map<IArgumentMatcher, BaseMockOperation> labelNotInMap = new HashMap<>();
  private Map<IArgumentMatcher, BaseMockOperation> fieldMap = new HashMap<>();
  private Map<IArgumentMatcher, BaseMockOperation> filedsMap = new HashMap<>();

  public Void replay() {
    for (Mockable mockable : nested) {
      mockable.replay();
    }
    EasyMock.replay(delegate);
    return null;
  }

  public void verify() {
    for (Mockable mockable : nested) {
      mockable.verify();
    }

    EasyMock.verify(delegate);
  }

  public BaseMockOperation newInstance() {
    try {
      return getClass().newInstance();
    } catch (Throwable t) {
      throw new RuntimeException();
    }
  }

  public ClientMixedOperation<C, T, L, D, R> getDelegate() {
    return delegate;
  }

  @Override
  public IExpectationSetters<T> create(T... item) {
    return expect(delegate.create(item));
  }

  @Override
  public B createNew() {
    B mock = null;
    try {
      mock = (B) mockDoneableType.newInstance();
      expect(delegate.createNew()).andReturn((D) mock.getDelegate()).once();
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
    if (mock instanceof Mockable) {
      nested.add((Mockable) mock);
    }
    return mock;
  }

  @Override
  public IExpectationSetters<Boolean> delete() {
    return expect(delegate.delete());
  }

  @Override
  public B edit() {
    B mock = null;
    try {
      mock = (B) mockDoneableType.newInstance();
      expect(delegate.edit()).andReturn((D) mock.getDelegate()).once();
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
    if (mock instanceof Mockable) {
      nested.add((Mockable) mock);
    }
    return mock;
  }

  @Override
  public E withName(String name) {
    IArgumentMatcher matcher = getArgument(name);
    BaseMockOperation<C, T, L, D, B, R, E> op = nameMap.get(matcher);
    if (op == null) {
      op = newInstance();
      expect(delegate.withName(name)).andReturn((R) op.getDelegate()).anyTimes();
      nested.add(op);
      nameMap.put(matcher, op);
    }
    return (E) op;
  }

  @Override
  public MockNonNamespaceOperation<T, L, B, E> inNamespace(String namespace) {
    IArgumentMatcher matcher = getArgument(namespace);
    BaseMockOperation<C, T, L, D, B, R, E> op = namespaceMap.get(matcher);
    if (op == null) {
      op = newInstance();
      expect(delegate.inNamespace(namespace)).andReturn((ClientNonNamespaceOperation<C, T, L, D, R>) op.getDelegate()).anyTimes();
      nested.add(op);
      namespaceMap.put(matcher, op);
    }
    return op;
  }

  @Override
  public MockNonNamespaceOperation<T, L, B, E> inAnyNamespace() {
    if (allNamespacesOp == null) {
      allNamespacesOp = newInstance();
      expect(delegate.inAnyNamespace()).andReturn((ClientNonNamespaceOperation<C, T, L, D, R>) allNamespacesOp.getDelegate()).anyTimes();
      nested.add(allNamespacesOp);
    }
    return allNamespacesOp;
  }

  @Override
  public EditReplaceDeletable<T, IExpectationSetters<T>, B, IExpectationSetters<Boolean>> cascading(boolean enabled) {
    IArgumentMatcher matcher = getArgument(enabled);
    BaseMockOperation<C, T, L, D, B, R, E> op = cascadingMap.get(matcher);
    if (op == null) {
      op = newInstance();
      expect(delegate.cascading(enabled)).andReturn(op.getDelegate()).anyTimes();
      nested.add(op);
      namespaceMap.put(matcher, op);
    }
    return op;
  }

  @Override
  public IExpectationSetters<T> update(T item) {
    return replace(item);
  }

  @Override
  public IExpectationSetters<T> replace(T item) {
    return null;
  }

  @Override
  public IExpectationSetters<T> get() {
    return expect(delegate.get());
  }

  @Override
  public Watch watch(Watcher<IExpectationSetters<T>> watcher) throws KubernetesClientException {
    return watch(null, watcher);
  }

  @Override
  public Watch watch(String resourceVersion, Watcher<IExpectationSetters<T>> watcher) throws KubernetesClientException {
    throw new UnsupportedOperationException();
  }

  @Override
  public FilterWatchListDeletable<IExpectationSetters<T>, IExpectationSetters<L>, IExpectationSetters<Boolean>> withLabels(Map<String, String> l) {
    IArgumentMatcher matcher = getArgument(l);
    BaseMockOperation<C, T, L, D, B, R, E> op = labelsMap.get(matcher);
    if (op == null) {
      op = newInstance();
      expect(delegate.withLabels(l)).andReturn(op.getDelegate()).anyTimes();
      nested.add(op);
      labelsMap.put(matcher, op);
    }
    return op;
  }

  @Override
  public FilterWatchListDeletable<IExpectationSetters<T>, IExpectationSetters<L>, IExpectationSetters<Boolean>> withoutLabels(Map<String, String> l) {
    IArgumentMatcher matcher = getArgument(l);
    BaseMockOperation<C, T, L, D, B, R, E> op = labelsNotMap.get(matcher);
    if (op == null) {
      op = newInstance();
      expect(delegate.withoutLabels(l)).andReturn(op.getDelegate()).anyTimes();
      nested.add(op);
      labelsNotMap.put(matcher, op);
    }
    return op;
  }

  @Override
  public FilterWatchListDeletable<IExpectationSetters<T>, IExpectationSetters<L>, IExpectationSetters<Boolean>> withLabelIn(String key, String... values) {
    IArgumentMatcher keyMatcher = getArgument(key);
    IArgumentMatcher valueMatcher = getArgument(values);
    IArgumentMatcher matcher = new And(Arrays.asList(keyMatcher, valueMatcher));

    BaseMockOperation<C, T, L, D, B, R, E> op = labelInsMap.get(matcher);
    if (op == null) {
      op = newInstance();
      expect(delegate.withLabelIn(key, values)).andReturn(op.getDelegate()).anyTimes();
      nested.add(op);
      labelInsMap.put(matcher, op);
    }
    return op;
  }

  @Override
  public FilterWatchListDeletable<IExpectationSetters<T>, IExpectationSetters<L>, IExpectationSetters<Boolean>> withLabelNotIn(String key, String... values) {
    IArgumentMatcher keyMatcher = getArgument(key);
    IArgumentMatcher valueMatcher = getArgument(values);
    IArgumentMatcher matcher = new And(Arrays.asList(keyMatcher, valueMatcher));

    BaseMockOperation<C, T, L, D, B, R, E> op = labelNotInMap.get(matcher);
    if (op == null) {
      op = newInstance();
      expect(delegate.withLabelNotIn(key, values)).andReturn(op.getDelegate()).anyTimes();
      nested.add(op);
      labelNotInMap.put(matcher, op);
    }
    return op;
  }

  @Override
  public FilterWatchListDeletable<IExpectationSetters<T>, IExpectationSetters<L>, IExpectationSetters<Boolean>> withLabel(String key, String value) {
    IArgumentMatcher keyMatcher = getArgument(key);
    IArgumentMatcher valueMatcher = getArgument(value);
    IArgumentMatcher matcher = new And(Arrays.asList(keyMatcher, valueMatcher));

    BaseMockOperation<C, T, L, D, B, R, E> op = labelMap.get(matcher);
    if (op == null) {
      op = newInstance();
      expect(delegate.withLabel(key, value)).andReturn(op.getDelegate()).anyTimes();
      nested.add(op);
      labelMap.put(matcher, op);
    }
    return op;
  }

  @Override
  public FilterWatchListDeletable<IExpectationSetters<T>, IExpectationSetters<L>, IExpectationSetters<Boolean>> withoutLabel(String key, String value) {
    IArgumentMatcher keyMatcher = getArgument(key);
    IArgumentMatcher valueMatcher = getArgument(value);
    IArgumentMatcher matcher = new And(Arrays.asList(keyMatcher, valueMatcher));

    BaseMockOperation<C, T, L, D, B, R, E> op = labelNotMap.get(matcher);
    if (op == null) {
      op = newInstance();
      expect(delegate.withoutLabel(key, value)).andReturn(op.getDelegate()).anyTimes();
      nested.add(op);
      labelNotMap.put(matcher, op);
    }
    return op;
  }

  @Override
  public FilterWatchListDeletable<IExpectationSetters<T>, IExpectationSetters<L>, IExpectationSetters<Boolean>> withFields(Map<String, String> f) {
    IArgumentMatcher matcher = getArgument(f);
    BaseMockOperation<C, T, L, D, B, R, E> op = filedsMap.get(matcher);
    if (op == null) {
      op = newInstance();
      expect(delegate.withFields(f)).andReturn(op.getDelegate()).anyTimes();
      nested.add(op);
      filedsMap.put(matcher, op);
    }
    return op;
  }

  @Override
  public FilterWatchListDeletable<IExpectationSetters<T>, IExpectationSetters<L>, IExpectationSetters<Boolean>> withField(String key, String value) {
    IArgumentMatcher keyMatcher = getArgument(key);
    IArgumentMatcher valueMatcher = getArgument(value);
    IArgumentMatcher matcher = new And(Arrays.asList(keyMatcher, valueMatcher));

    BaseMockOperation<C, T, L, D, B, R, E> op = fieldMap.get(matcher);
    if (op == null) {
      op = new BaseMockOperation<>();
      expect(delegate.withField(key, value)).andReturn(op.getDelegate()).anyTimes();
      nested.add(op);
      fieldMap.put(matcher, op);
    }
    return op;
  }

  @Override
  public IExpectationSetters<L> list() {
    return expect(delegate.list());
  }


  public Set<Mockable> getNested() {
    return nested;
  }

  @Override
  public MockResource load(InputStream input) {
    if (loadedMockOp == null) {
      loadedMockOp = newInstance();
    }
    expect(delegate.load(input)).andReturn((R) loadedMockOp.getDelegate()).anyTimes();
    nested.add(loadedMockOp);
    return loadedMockOp;
  }

  @Override
  public IExpectationSetters<Boolean> delete(T... items) {
    return expect(delegate.delete(items));
  }

  @Override
  public IExpectationSetters<Boolean> delete(List<T> items) {
    return expect(delegate.delete(items));
  }
}
