/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package org.syncope.console.wicket.markup.html.form;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;

/**
 * ListMultipleChoiceTransfer Wicket component.
 */
public abstract class ListMultipleChoiceTransfer extends Panel {
  
  //The list choices.
  private ListMultipleChoice originals;
  private ListMultipleChoice destinations;

  // The Selected options in the list choices.
  public List<String> selectedOriginals  = new ArrayList<String>();
  public List<String> selectedDestinations  = new ArrayList<String>();

  // The Buttons that contain actions.
  private AjaxButton add;
  private AjaxButton remove;

  /**
   * Default constructor.
   * @param Wicket placeholder id
   * @param Widget's title
   */
  public ListMultipleChoiceTransfer(String id, String originalsName,
          String destinationsName) {
    super(id);

    originals = new ListMultipleChoice("originals", new PropertyModel(this,"selectedOriginals"), setupOriginals());
    originals.setOutputMarkupId(true);
    add(originals);

    destinations = new ListMultipleChoice("destinations", new PropertyModel(this, "selectedDestinations"), setupDestinations());
    destinations.setOutputMarkupId(true);
    add(destinations);

    add(new Label("originalsName",originalsName));

    add(new Label("destinationsName",destinationsName));

    add = new AjaxButton("add") {
      @Override
      protected void onSubmit(AjaxRequestTarget target, Form form) {
        update(target,originals.getChoices(), originals, destinations);
      }
    };
    add.setDefaultFormProcessing(false);
    add(add);

    remove = new AjaxButton("remove") {
      @Override
      protected void onSubmit(AjaxRequestTarget target, Form form) {
        update(target, destinations.getChoices(), destinations, originals);
      }
    };
    remove.setDefaultFormProcessing(false);
    add(remove);
  }

  /**
   * Updates the select boxes.
   * @param target The {@link AjaxRequestTarget}.
   */
  private void update(AjaxRequestTarget target, List<String> selections, ListMultipleChoice from, ListMultipleChoice to) {
    for (String selection : selections) {
      List<String> choices = getChoices(from);
      if (!to.getChoices().contains(selection)) {
        to.getChoices().add(selection);
        choices.remove(selection);
        from.setChoices(choices);
      }
    }
    target.addComponent(to);
    target.addComponent(from);
  }

  /**
   * Retrieves a {@link List} of choices for the given {@link ListMultipleChoice}. Because the
   * {@link AbstractList} method remove is a stub so we need to <br />
   * create a new List so we can remove the item.
   *
   * @return
   */
  @SuppressWarnings("unchecked")
  private List<String> getChoices(ListMultipleChoice choice) {
    List<String> choices = new ArrayList<String>();
    choices.addAll(choice.getChoices());
    return choices;
  }

   /**
    * It's used for getting final user's selections.
    * @return List<String>
    */
    public List<String> getFinalSelections() {
        return originals.getChoices();
    }    

  /**
   * Setup originals list.
   * @return List<String>
   */
  public abstract List<String> setupOriginals();

  /**
   * Setup originals list.
   * @return List<String
   */
  public abstract List<String> setupDestinations();

}