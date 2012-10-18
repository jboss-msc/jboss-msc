/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.msc.txn;

/**
 * A task which can be validated before commit.  When validation occurs, all work is complete; all
 * dependencies will have been validated, and all dependents will await the results of this validation
 * before proceeding.  If validation fails, dependents will not be validated.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface Validatable {

    /**
     * Perform validation.  Any problems should be reported via the {@code validateContext}.  When validation
     * is complete, the context's {@code complete()} method must be called, regardless of whether validation
     * succeeded or failed.
     *
     * @param validateContext the validation context
     */
    void validate(ValidateContext validateContext);
}
