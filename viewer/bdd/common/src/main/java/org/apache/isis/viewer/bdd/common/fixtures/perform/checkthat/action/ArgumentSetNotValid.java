package org.apache.isis.viewer.bdd.common.fixtures.perform.checkthat.action;

import java.util.List;

import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.consent2.Consent;
import org.apache.isis.core.metamodel.spec.feature.ObjectAction;
import org.apache.isis.core.metamodel.spec.feature.ObjectMember;
import org.apache.isis.viewer.bdd.common.CellBinding;
import org.apache.isis.viewer.bdd.common.ScenarioBoundValueException;
import org.apache.isis.viewer.bdd.common.ScenarioCell;
import org.apache.isis.viewer.bdd.common.fixtures.perform.PerformContext;
import org.apache.isis.viewer.bdd.common.fixtures.perform.checkthat.ThatSubcommandAbstract;

public class ArgumentSetNotValid extends ThatSubcommandAbstract {

	public ArgumentSetNotValid() {
		super("is not valid for", "is invalid", "invalid");
	}

	// TODO: a lot of duplication with InvokeAction; simplify somehow?
	public ObjectAdapter that(final PerformContext performContext)
			throws ScenarioBoundValueException {

		final ObjectAdapter onAdapter = performContext.getOnAdapter();
		final ObjectMember nakedObjectMember = performContext
				.getObjectMember();
		final CellBinding onMemberBinding = performContext.getPeer()
				.getOnMemberBinding();
		final List<ScenarioCell> argumentCells = performContext.getArgumentCells();

		final ObjectAction nakedObjectAction = (ObjectAction) nakedObjectMember;
		final int parameterCount = nakedObjectAction.getParameterCount();
		final boolean isContributedOneArgAction = nakedObjectAction
				.isContributed()
				&& parameterCount == 1;

		if (isContributedOneArgAction) {
			return null;
		}

		// lookup arguments
		final ObjectAdapter[] proposedArguments = performContext.getPeer()
				.getAdapters(onAdapter, nakedObjectAction, onMemberBinding, argumentCells);

		// validate arguments
		final Consent argSetValid = nakedObjectAction
				.isProposedArgumentSetValid(onAdapter, proposedArguments);
		if (argSetValid.isAllowed()) {
			CellBinding thatItBinding = performContext.getPeer()
					.getThatItBinding();
			throw ScenarioBoundValueException.current(thatItBinding, "(valid)");
		}

		// execute
		return null;
	}

}
