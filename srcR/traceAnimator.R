#!R 
animateTrace<-function(x,y,z,frames=100,phi=.01,zoom=1,rotSpeed=1,stheta=0,start=0,end=length(x),radius=1,xlab='x',ylab='y',zlab='z'){
	#first we need to get the traces...
	nans<-is.nan(x)
	delims<-which(nans)
	delLen<-length(delims)
	maxLength<-max(delims[2:delLen]-delims[1:delLen-1])
	maxLength<-min(maxLength,end)

	reg<-delims+start	
	delta<-maxLength/frames
	
	plot3d(x,y,z,type='n',xlab=xlab,ylab=ylab,zlab=zlab)
	theta=25;
	view3d(theta+stheta,phi,zoom=zoom)
	for(c in 1:frames){
		
		#cat('frame ',c,'\nML:',maxLength,'\n')
		nreg<-reg+delta
		#cat('nc,c ',reg,'\t',nreg,'\ndelta:',delta,'\n\n')
		r<-c()
		p<-c()
		for(i in 1:length(reg)){
			if(nreg[i]<length(x) && i<length(reg) && nreg[i]<delims[i+1]){
				r<-c(r,1,seq(reg[i],nreg[i]))
				p<-c(p,nreg[i])
			}
			#cat('range:',r,'\t',i,'\n')
		}
		if(c>1)
			rgl.pop()
		#plot3d(x[3,],x[1,],x[2,],type='n')
		lines3d(x[r],y[r],z[r],col=heat.colors(maxLength)[reg[1]],lwd=1.5)
		spheres3d(x[p],y[p],z[p],col='blue',radius=radius)
		reg<-nreg
		view3d(theta+stheta,phi,zoom=zoom)
		theta<-theta+rotSpeed
	}
}
