package uk.ac.cam.cl.dtg.isaac.quiz;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Lists;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacGraphSketcherQuestion;
import org.isaacphysics.graphchecker.data.Input;
import org.isaacphysics.graphchecker.dos.GraphAnswer;
import org.isaacphysics.graphchecker.features.Features;
import org.isaacphysics.graphchecker.translation.AnswerToInput;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.GraphChoice;
import uk.ac.cam.cl.dtg.segue.dos.content.Question;
import uk.ac.cam.cl.dtg.segue.quiz.ISpecifier;
import uk.ac.cam.cl.dtg.segue.quiz.IValidator;
import uk.ac.cam.cl.dtg.segue.quiz.ValidatorUnavailableException;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * Validator that only provides functionality to validate graph questions.
 *
 * Created by hhrl2 on 01/08/2016.
 */
public class IsaacGraphSketcherValidator implements IValidator, ISpecifier {

    /**
     * Private logger for printing error messages on console.
     */
    private static final Logger log = LoggerFactory.getLogger(IsaacGraphSketcherValidator.class);

    private static final AnswerToInput answerToInput = new AnswerToInput();
    private static final Features features = new Features();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public final QuestionValidationResponse validateQuestionResponse(final Question question, final Choice answer) {
        Validate.notNull(question);
        Validate.notNull(answer);

        if (!(question instanceof IsaacGraphSketcherQuestion)) {
            throw new IllegalArgumentException(String.format(
                "This validator only works with Isaac Graph Sketcher Questions... (%s is not string match)",
                question.getId()));
        }

        if (!(answer instanceof GraphChoice)) {
            throw new IllegalArgumentException(String.format(
                "Expected GraphChoice for IsaacGraphSketcherQuestion: %s. Received (%s) ", question.getId(),
                answer.getClass()));
        }

        IsaacGraphSketcherQuestion graphSketcherQuestion = (IsaacGraphSketcherQuestion) question;

        // These variables store the important features of the response we'll send.
        Content feedback = null;                        // The feedback we send the user
        boolean responseCorrect = false;                // Whether we're right or wrong

        if (null == graphSketcherQuestion.getChoices() || graphSketcherQuestion.getChoices().isEmpty()) {
            log.error("Question does not have any answers. " + question.getId() + " src: "
                + question.getCanonicalSourceFile());

            feedback = new Content("This question does not have any correct answers");
        }

        // STEP 1: Did they provide an answer at all?

        GraphAnswer graphAnswer = null;

        if (null == feedback && (null == answer.getValue() || answer.getValue().isEmpty())) {
            feedback = new Content("You did not provide an answer");
        } else {
            try {
                graphAnswer = objectMapper.readValue(answer.getValue(), GraphAnswer.class);
            } catch (IOException e) {
                log.error("Expected a GraphAnswer, but couldn't parse it for question id: "
                    + graphSketcherQuestion.getId(), e);
                feedback = new Content("Your graph could not be read");
            }
        }

        // STEP 2: If they did, does their answer match a known answer?

        if (null == feedback) {

            Input input = answerToInput.apply(graphAnswer);

            // Sort the choices so that we match incorrect choices last, taking precedence over correct ones.
            List<Choice> orderedChoices = getOrderedChoices(graphSketcherQuestion.getChoices());

            // For all the choices on this question...
            for (Choice c : orderedChoices) {

                // ... that are of the GraphChoice type, ...
                if (!(c instanceof GraphChoice)) {
                    log.error("Isaac GraphSketcher Validator for questionId: " + graphSketcherQuestion.getId()
                        + " expected there to be a GraphChoice . Instead it found a Choice.");
                    continue;
                }
                GraphChoice graphChoice = (GraphChoice) c;

                if (null == graphChoice.getGraphSpec() || graphChoice.getGraphSpec().isEmpty()) {
                    log.error("Expected a spec to match, but none found in choice for question id: "
                        + graphSketcherQuestion.getId());
                    continue;
                }

                Features.Matcher matcher = features.matcher(graphChoice.getGraphSpec());

                if (matcher.test(input)) {
                    feedback = (Content) graphChoice.getExplanation();
                    responseCorrect = graphChoice.isCorrect();
                    break;
                }
            }
        }

        // STEP 3: If we still have no feedback to give, use the question's default feedback if any to use:
        if (feedbackIsNullOrEmpty(feedback) && null != graphSketcherQuestion.getDefaultFeedback()) {
            feedback = graphSketcherQuestion.getDefaultFeedback();
        }

        return new QuestionValidationResponse(question.getId(), answer, responseCorrect, feedback, new Date());
    }

    /**
     * Create a new list of Choice objects, with no change in sort order.
     *
     * This is desirable if you ever need to use a feature in a negative way to identify an incorrect answer.
     *
     * @param choices - the Choices from a Question
     * @return the ordered list of Choices
     */
    public List<Choice> getOrderedChoices(final List<Choice> choices) {
        return Lists.newArrayList(choices);
    }

    @Override
    public String createSpecification(Choice answer) throws ValidatorUnavailableException {
        if (!(answer instanceof GraphChoice)) {
            log.error("Isaac GraphSketcher specifier expected there to be a GraphChoice . Instead it found a Choice.");
            throw new ValidatorUnavailableException("Incorrect choice type");
        }
        GraphChoice graphChoice = (GraphChoice) answer;

        GraphAnswer graphAnswer = null;

        try {
            graphAnswer = objectMapper.readValue(answer.getValue(), GraphAnswer.class);
        } catch (IOException e) {
            log.error("Expected a GraphAnswer, but couldn't parse it for specification", e);
            throw new ValidatorUnavailableException("Couldn't parse your GraphAnswer");
        }

        Input input = answerToInput.apply(graphAnswer);

        return features.generate(input);
    }
}