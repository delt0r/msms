%.pdf: %.dvi 
	dvipdf $<

%.dvi: %.tex *.eps
	latex $*
	bibtex $*
	latex $*
	latex $*

clean:
	rm -f *.aux *.log  *.dvi *.bbl *.log *.blg 
