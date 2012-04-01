#!R 
animateTrace<-function(x,frames=100){
	#first we need to get the traces...
	nans<-is.nan(x[1,])
	delims<-which(nans)
	delLen<-length(delims)
	maxLength<-max(delims[2:delLen]-delims[1:delLen-1])
	#cat(delims)	
	#limx<-c(min(x[1,!nans]),max(x[1,!nans]))
	#limy<-c(min(x[2,!nans],max(x[2,!nans])))
	#limz<-c(min(x[3,!nans],max(x[3,!nans])))

	reg<-delims	
	delta<-maxLength/frames
	
	plot3d(x[3,],x[1,],x[2,],type='n')
	theta=25;
	for(c in 1:frames){
		#cat('frame ',c,'\nML:',maxLength,'\n')
		nreg<-reg+delta
		#cat('nc,c ',reg,'\t',nreg,'\ndelta:',delta,'\n\n')
		r<-c()
		p<-c()
		for(i in 1:length(reg)){
			r<-c(r,1,seq(delims[i],nreg[i]))
			p<-c(p,nreg[i])
			#cat('range:',r,'\t',i,'\n')
		}
		plot3d(x[3,],x[1,],x[2,],type='n')
		lines3d(x[3,r],x[1,r],x[2,r],col=heat.colors(maxLength)[reg[1]],lwd=1.5)
		spheres3d(x[3,p],x[1,p],x[2,p],col='blue',radius=3)
		reg<-nreg
		#view3d(theta,0)
		#theta<-theta+1
	}
}
