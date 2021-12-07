# TermProjectCS583

Creates an index if needed but currently has five indexes made, to choose from and use to answer
queries from the text file "questions.txt" that contains 100 Jeopardy questions

Five indexes:
	"standard.txt"
	"standLemma.txt"
	"english.txt"
	"engLemma.txt"
	"bestDiff.txt"
	
The first two indexes use a Standard Analyzer (does not do stemming)
The first does no Lemmatize and the second does

The remaining 3 use English Analyzer (does stemming)
"english.txt" does not lemmatize, the last two lemmatize input
and the very last one is indexed with a different scoring function

Counts of how many where answered correctly and how many fall within top 2, 10 and 100 for error analysis

Through a boolean variable:
	Has the option to perform re-ranking which looks to change top 2 or top 3
	Has option to lemmatize the index (input) and query
	Has option to change the scoring function 
	Needs to be told true when asking for error analysis (percent correct)
	
Performance is measured by keeping track of where the real answer if ranked and
taking the inverse of each questions ranking, summing it up and dividing by the total number of questions

We are given the real answer in "questions.txt"

The query format is category plus the second "half" of the clue portion (also in "questions.txt")

The main function has examples of opening and answering queries using two indexes with and without re-ranking