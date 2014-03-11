for x in *.out; do trec_eval.9.0/trec_eval test-data/qrels.trec6-8.nocr "$x" > "result/default_${x%.out}.eval"; done
for x in result*.out; do touch "default_${x%.out}.eval"; done
for x in *.eval; do mv -- "$x" "${x%.eval}.out"; done