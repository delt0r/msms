#!R 
animateTrace<-function(x,y,z,frames=100,phi=.01,zoom=1,rotSpeed=1,stheta=0,start=0,end=length(x),radius=1,traces=NaN,xlab='x',ylab='y',zlab='z',SnapShots=FALSE){
	#first we need to get the traces...add a NaN at the end just to make sure we have a termination NaN
	x<-c(x,NaN)
	y<-c(y,NaN)
	z<-c(z,NaN)
	nans<-is.nan(x)
	delims<-which(nans)
	
	if(is.nan(traces)){
		traces<-length(delims)
	}
	
	delLen<-length(delims)
	maxLength<-max(delims[2:delLen]-delims[1:delLen-1])
	maxLength<-min(maxLength,end-start)

	reg<-delims+start	
	delta<-maxLength/frames
	
	r<-c()
	for(i in 1:(traces)){
		#cat('regi:',reg[i],' minStuff:',min(reg[i]+maxLength,delims[i+1]),'\n')
		top<-min(reg[i]+maxLength,delims[i+1])
		if(i==traces)
			top=length(x)
		r<-c(r,1,seq(reg[i],top))
	}
	#cat('What are we printing:',r,' ml:',maxLength)
	plot3d(x[r],y[r],z[r],type='n',xlab=xlab,ylab=ylab,zlab=zlab)
	theta=25;
	view3d(theta+stheta,phi,zoom=zoom)
	framecounter<-10000
	for(c in 1:frames){
		
		#cat('frame ',c,'\nML:',maxLength,'\n')
		nreg<-delims+c*delta+start
		#cat('delta:',delta*c)
		#cat('nc,c ',reg,'\t',nreg,'\ndelta:',delta,'\n\n')
		r<-c()
		p<-c()
		for(i in 1:traces){
			top<-min(nreg[i],delims[i+1])
			if(i==traces)
				top=length(x)
			if(reg[i]<top){
				r<-c(r,1,seq(reg[i],top))
				p<-c(p,top)
			}
		}
		view3d(theta+stheta,phi,zoom=zoom)
		theta<-theta+rotSpeed
		
		
		#plot3d(x[3,],x[1,],x[2,],type='n')
		lines3d(x[r],y[r],z[r],col=heat.colors(frames)[c],lwd=1.5)
		spheres3d(x[p],y[p],z[p],col='blue',radius=radius)
		if(SnapShots){
			rgl.snapshot(filename=paste('snap',framecounter+c,'.png',sep=''))
		}
		rgl.pop()
		reg<-nreg-1
	}
}
